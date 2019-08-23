(ns ct.chat.handler
  (:require
   [clojure.java.io :as io]
   [reitit.ring :as reitit-ring]
   [hiccup.page :refer [include-js include-css html5]]
   [config.core :refer [env]]
   [ring.util.anti-forgery :refer [anti-forgery-field]]
   [compojure.core :refer [routes GET POST]]
   [compojure.route :refer [not-found]]
   [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
   [prone.middleware :refer [wrap-exceptions]]
   [ring.middleware.reload :refer [wrap-reload]]
   [buddy.sign.jwt :as jwt]
   [clj-time.core :as time]
   [cheshire.core :as json]
   [ct.chat.middleware :refer [middleware]]))

(def mount-target
  [:div#app
   "Loading"])

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta
    {:name "viewport"
     :content "width=device-width, initial-scale=1"}]
   (include-css (if (env :dev) "/css/site.css" "/css/site.min.css"))])

(defn cards-page []
  (html5
   (head)
   [:body
    mount-target
    (include-js "/js/app_devcards.js")]))

(defn cards-handler [_]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (cards-page)})

(defn chat-page [{:keys [jid password]}]
  (html5
   (head)
   [:body {:class "body-container"}
    mount-target
    (include-js "/js/app.js")
    [:script {:type "text/javascript"}
     (let [chat-options {:jid jid
                         :password password
                         :roomJid "test-room@chat"
                         :serverJid "localhost"
                         :websocketUri "ws://localhost:5443/ws/"}]
       (format "window.ct.chat.initialize(%s);"
               (json/generate-string chat-options)))]]))

(defn ejabberd-auth-token [jid]
  (let [claims {:jid jid :exp (time/plus (time/now) (time/days 365))}]
    (jwt/sign claims "secret" {:alg :hs256})))

(defn username->jid [username] (str username "@localhost"))

(defn chat-handler [request]
  (if-let [username (get-in request [:session ::username])]
    (let [jid (username->jid username)]
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body (chat-page {:jid jid :password (ejabberd-auth-token jid)})})
    {:status 302
     :headers {"Location" "/login"}}))

(defn login-page [request]
  (html5
   (head)
   [:body
    [:form {:method "POST"}
     (anti-forgery-field)
     [:div
      [:label {:for "username"} "Username"]]
     [:div
      [:input#username {:type "text" :name "username"}]]
     [:div
      [:label {:for "password"} "Password"]]
     [:div
      [:input#password {:type "password" :name "password"}]]
     [:div
      [:button {:type "submit"} "Login"]]]]))

(defn login-handler [request]
  (if-let [jid (get-in request [:session ::jid])]
    {:status 302
     :headers {"Location" "/chat"}}
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (login-page request)}))

(def users
  {"test" "test"
   "josh" "test"
   "user" "test"})

(defn authorized-user? [username password]
  (and (users username) (= (users username) password)))

(defn login-post-handler [request]
  (let [username (get-in request [:form-params "username"])
        password (get-in request [:form-params "password"])]
    (if-not (authorized-user? username password)
      {:status 403
       :headers {"Content-Type" "text/html"}
       :body "Incorrect username or password"}
      {:status 302
       :session {::username username}
       :headers
       {"Content-Type" "text/html"
        "Location" "/chat"}})))

(defn index-handler [request]
  {:status 302
   :headers {"Location" "/chat"}})

(def app
  (-> (routes
       (GET "/" request (index-handler request))
       (GET "/chat" request (chat-handler request))
       (GET "/login" request (login-handler request))
       (POST "/login" request (login-post-handler request))
       (GET "/cards" request (cards-handler request))
       (not-found "Not Found"))
      (wrap-defaults site-defaults)
      (wrap-exceptions)
      (wrap-reload)))
