(ns ct.chat.media.connection
  (:require
   [cljs.core.async :refer [chan put! close!]]
   [protoo-client :as protoo]))

(defonce ^:private request-handlers (atom {}))

(defn- handle-request [request accept reject]
  (let [request (js->clj request :keywordize-keys true)]
    (if-let [handle-fn (get @request-handlers (keyword (:method request)))]
      (handle-fn request accept reject)
      (throw (ex-info "No handler for request" request)))))

(defn set-request-handler! [method handle-fn]
  (swap! request-handlers assoc method handle-fn)
  nil)

(defonce ^:private notification-handlers (atom {}))

(defn- handle-notification [notification]
  (let [notification (js->clj notification :keywordize-keys true)]
    (if-let [handle-fn (get @notification-handlers (keyword (:method notification)))]
      (handle-fn notification)
      (throw (ex-info "No handler for notification" notification)))))

(defn set-notification-handler! [method handle-fn]
  (swap! notification-handlers assoc method handle-fn)
  nil)

(defonce ^:private transport (atom nil))

(defonce ^:private peer (atom nil))

(defn close-connection! []
  (when @peer
    (.close @peer)
    (reset! peer nil)
    (reset! transport nil)))

(defn reset-connection! [url {:keys [on-open on-failed on-disconnected on-close]}]
  (close-connection!)
  (reset! transport (protoo/WebSocketTransport. url))
  (reset! peer (protoo/Peer. @transport))
  (doto @peer
    (.on "open" on-open)
    (.on "failed" on-failed)
    (.on "disconnected" on-disconnected)
    (.on "close" on-close)
    (.on "request" handle-request)
    (.on "notification" handle-notification)))

(defn request!
  ([method] (request! method nil))
  ([method data]
   (let [ch (chan)]
     (-> (.request @peer (name method) (clj->js data))
         (.then (fn [response] (put! ch (js->clj response :keywordize-keys true))))
         (.catch (fn [error]
                   (close! ch)
                   (throw error))))
     ch)))

(defn notify!
  ([method] (notify! method nil))
  ([method data]
   (let [ch (chan)]
     (-> (.notify @peer (name method) (clj->js data))
         (.then (fn [response] (put! ch (js->clj response :keywordize-keys true))))
         (.catch (fn [error] (close! ch))))
     ch)))
