(ns ct.chat.media.events
  (:require
   [re-frame.core :as rf]
   [ct.chat.xmpp.stanzas.muc :refer [muc-room-presence-stanza]]
   [ct.chat.xmpp.stanzas.media
    :refer [producer-presence-stanza
            has-child-media-producer-element?
            child-media-producer-element]]
   [ct.chat.media.signalling.effects :as signalling]
   [ct.chat.media.effects :as fx]))

(rf/reg-event-fx
 ::initialize
 (fn [{:keys [db]} _]
   {:dispatch
    [::notification-handlers-ready]
    :xmpp/add-listener
    {:id ::presence-with-producer
     :xform (comp (filter has-child-media-producer-element?)
                  (map child-media-producer-element))
     :on-message [::producer-received]}
    ::signalling/set-notification-handlers
    {:ready [::ready-notification-received]}}))

(rf/reg-event-fx
 ::notification-handlers-ready
 (fn [{:keys [db]} _]
   {::signalling/initialize {:url (str (:media/signalling-websocket-uri db))}}))

(rf/reg-event-fx
 ::ready-notification-received
 (fn [_ _]
   {::signalling/request
    {:method :capabilities
     :on-success [::capabilities-received]}}))

(rf/reg-event-fx
 ::capabilities-received
 (fn [_ [_ capabilities]]
   {::fx/load-device
    {:capabilities capabilities
     :on-loaded [::device-loaded]}}))

(rf/reg-event-fx
 ::device-loaded
 (fn [_ _]
   {::signalling/request
    {:method :create-receive-transport
     :on-success [::receive-transport-created]}}))

(rf/reg-event-fx
 ::receive-transport-created
 (fn [_ [_ parameters]]
   {::fx/create-receive-transport {:parameters (.-receiveTransport parameters)}}))

;; CONSUMING
;; ---------

(rf/reg-event-fx
 ::producer-received
 (fn [_ [_ producer-element]]
   {::signalling/request
    {:method :create-consumer
     :data {:producerId (get-in producer-element [:attrs :id])}
     :on-success [::server-consumer-created]}}))

(rf/reg-event-fx
 ::server-consumer-created
 (fn [_ [_ parameters]]
   (let [consumer-id (.-id parameters)]
     {::fx/create-consumer
      {:parameters parameters
       :on-ready [::consumer-ready consumer-id]
       :on-track-ended [::consumer-track-ended consumer-id]}})))

(rf/reg-event-fx
 ::consumer-ready
 (fn [{:keys [db]} [_ consumer-id]]
   {:db (update db :media/consumers assoc consumer-id {:id consumer-id})}))

;; BROADCASTING
;; ------------

(rf/reg-event-fx
 ::broadcast-error-occurred
 (fn [{:keys [db]} [_ message]]
   (js/window.alert (str "Broadcasting error: " message))
   {:db (cond-> db
          (= :preparing (:media/broadcast-state db))
          (assoc :media/broadcast-state nil))}))

(rf/reg-event-fx
 ::broadcast-requested
 (fn [{:keys [db]} _]
   {:db (assoc db :media/broadcast-state :preparing)
    ::fx/request-user-media
    {:on-success [::user-media-ready]
     :on-error [::broadcast-error-occurred "Could not access devices."]
     :on-timeout [::broadcast-error-occurred "Permissions request timed out."]}}))

(rf/reg-event-fx
 ::user-media-ready
 (fn [_ _]
   {::signalling/request
    {:method :create-send-transport
     :on-success [::server-send-transport-created]}}))

(rf/reg-event-fx
 ::server-send-transport-created
 (fn [_ [_ parameters]]
   {::fx/create-send-transport {:parameters (.-sendTransport parameters)}
    :dispatch [::send-transport-ready]}))

(rf/reg-event-fx
 ::send-transport-ready
 (fn [_ _]
   {::fx/broadcast-webcam {:on-ready [::producer-ready]}}))

(rf/reg-event-fx
 ::producer-ready
 (fn [{:keys [db]} [_ producer-id]]
   (let [{:connection/keys [full-jid]
          :chats/keys [chats active-chat-jid]}
         db]
     {:db (assoc db :media/broadcast-state :broadcasting)
      :xmpp/send
      {:stanza
       (producer-presence-stanza
        {:from full-jid
         :to (get-in chats [active-chat-jid :from-jid])
         :producer-id producer-id})}})))
