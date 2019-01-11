(ns ct.xmpp.effects
  (:require
   [cljs.core.async :as async :refer [chan tap untap go go-loop <! alt! close!]]
   [clojure.data.xml :as xml]
   [re-frame.core :as rf]
   [ct.xmpp.connection :refer [reset-connection!
                               close-connection!
                               send!
                               messages-mult]]))

(rf/reg-fx
 :xmpp/connect
 (fn [{:keys [url on-open on-error] :or {on-error [:xmpp/connect.error]}}]
   (reset-connection! url {:on-open #(rf/dispatch on-open)
                           :on-error #(rf/dispatch on-error)})))

(rf/reg-fx
 :xmpp/close
 (fn [_]
   (close-connection!)))

(rf/reg-fx
 :xmpp/send
 (fn [{:keys [stanza]}]
   (send! stanza)))

(rf/reg-fx
 :xmpp/send-then-listen
 (fn [{:keys [stanza xform on-message timeout on-timeout]
       :or {timeout 1000
            on-timeout [:xmpp/send-then-listen.timeout]}}]
   (let [message-ch (chan 1 xform)
         timeout-ch (async/timeout timeout)]
     (tap messages-mult message-ch)
     (send! stanza)
     (go
       (rf/dispatch
        (alt!
          message-ch ([message] (conj on-message message))
          timeout-ch on-timeout))
       (untap messages-mult message-ch)))))

(def ^:private default-ns "jabber:client")

(defn- iq-response? [type id stanza]
  (and (= (xml/qname default-ns :iq) (:tag stanza))
       (= (name type) (get-in stanza [:attrs :type]))
       (= id (get-in stanza [:attrs :id]))))

(defn- iq-stanza [attrs content]
  (xml/element (xml/qname default-ns :iq) (update attrs :type name) content))

(rf/reg-fx
 :xmpp/query
 (fn [{:keys [type content from to on-result on-error timeout on-timeout]
       :or {timeout 1000
            on-error [:xmpp/query.error]
            on-timeout [:xmpp/query.timeout]}}]
   (let [id (str (random-uuid))
         result-ch (chan 1 (filter (partial iq-response? :result id)))
         error-ch (chan 1 (filter (partial iq-response? :error id)))
         timeout-ch (async/timeout timeout)]
     (tap messages-mult result-ch)
     (tap messages-mult error-ch)
     (send! (iq-stanza
             (cond-> {:type type :id id}
               from (assoc :from from)
               to (assoc :to to))
             content))
     (go
       (rf/dispatch
        (alt!
          result-ch ([result] (conj on-result result))
          error-ch ([error] (conj on-error error))
          timeout-ch on-timeout))
       (untap messages-mult result-ch)
       (untap messages-mult error-ch)))))

(defonce ^:private listener-close-channels (atom {}))

(rf/reg-fx
 :xmpp/add-listener
 (fn [{:keys [id xform on-message]}]
   (let [message-ch (chan 1 xform)
         close-ch (chan)]
     (tap messages-mult message-ch)
     (swap! listener-close-channels assoc id close-ch)
     (go-loop []
       (alt!
         message-ch ([message] (rf/dispatch (conj on-message message)) (recur))
         close-ch ([] (untap messages-mult message-ch)))))))

(rf/reg-event-fx
 :xmpp/stop-listener
 (fn [{:keys [id]}]
   (close! (@listener-close-channels id))
   (swap! listener-close-channels dissoc id)))
