(ns ct.chat.connection.events
  (:require
   [re-frame.core :as rf]
   [ct.chat.xmpp.stanzas.framing
    :refer [initial-open-stanza
            open-stanza]]
   [ct.chat.xmpp.stanzas.streams :refer [features-stanza?]]
   [ct.chat.xmpp.stanzas.sasl
    :refer [features-supports-sasl-plain?
            sasl-plain-auth-stanza
            sasl-auth-success?]]
   [ct.chat.xmpp.stanzas.binding
    :refer [features-supports-bind?
            bind-query-content
            bind-jid]]
   [ct.chat.xmpp.stanzas.presence :refer [presence-stanza]]))

(rf/reg-event-fx
 ::connection-error-occured
 (fn [_ [_ message]]
   (js/window.alert (str "Connection error! " message))
   {:xmpp/close nil}))

;; XMPP Stream Initialization
;; ==========================

(rf/reg-event-fx
 ::initialize
 (fn [{:keys [db]} [_ {:keys [on-ready]}]]
   (let [{:connection/keys [websocket-uri]} db]
     {:db (assoc db :connection/on-ready on-ready)
      :xmpp/connect {:url (str websocket-uri)
                     :on-open [::websocket-opened]
                     :on-error [::connection-error-occured
                                "Could not connect to the server."]}})))

(rf/reg-event-fx
 ::websocket-opened
 (fn [{:keys [db]} _]
   (let [{:connection/keys [server-bare-jid]} db]
     {:xmpp/send-then-listen
      {:stanza (initial-open-stanza {:to server-bare-jid})
       :on-message [::initial-features-received]
       :xform (filter features-stanza?)}})))

(rf/reg-event-fx
 ::initial-features-received
 (fn [{:keys [db]} [_ features-stanza]]
   (let [{:connection/keys [bare-jid password]} db]
     (if (features-supports-sasl-plain? features-stanza)
       {:xmpp/send-then-listen
        {:stanza (sasl-plain-auth-stanza bare-jid password)
         :on-message [::sasl-response-received]
         :on-timeout [::connection-error-occured "Authentication timed out."]}}
       {:dispatch [::connection-error-occured "Could not start authentication."]}))))

(rf/reg-event-fx
 ::sasl-response-received
 (fn [{:keys [db]} [_ sasl-response-stanza]]
   (let [{:connection/keys [bare-jid server-bare-jid]} db]
     (if (sasl-auth-success? sasl-response-stanza)
       {:xmpp/send-then-listen
        {:stanza (open-stanza {:from bare-jid :to server-bare-jid})
         :on-message [::bind-features-received]
         :xform (filter features-stanza?)}}
       {:dispatch [::connection-error-occured "Auth failure."]}))))

(rf/reg-event-fx
 ::bind-features-received
 (fn [_ [_ features-stanza]]
   (if (features-supports-bind? features-stanza)
     {:xmpp/query
      {:type :set
       :content (bind-query-content)
       :on-result [::bind-result-received]
       :on-error [::connection-error-occured "Could not complete resource binding."]
       :on-timeout [::connection-error-occured "Resource binding timed out."]}}
     {:dispatch [::connection-error-occured "Could not start resource binding."]})))

(rf/reg-event-fx
 ::bind-result-received
 (fn [{:keys [db]} [_ bind-result-stanza]]
   {:db (assoc db :connection/full-jid (bind-jid bind-result-stanza))
    :xmpp/send-then-listen
    {:stanza (presence-stanza)
     :on-message (:connection/on-ready db)}}))
