(ns ct.chat.media.events
  (:require
   [re-frame.core :as rf]
   [day8.re-frame.http-fx]
   [ajax.core :as ajax]
   [ct.chat.xmpp.stanzas.muc :refer [muc-presence?]]
   [ct.chat.xmpp.stanzas.media
    :refer [presence->producers
            media-presence-stanza]]
   [ct.chat.media.effects :as fx]))

(rf/reg-event-fx
 ::error-occurred
 (fn [_ [_ message]]
   (js/window.alert (str "Error: " message))
   {}))

(rf/reg-event-fx
 ::initialize
 (fn [{:keys [db]} _]
   {:xmpp/add-listener
    {:id ::room-presence-listener
     :xform (filter muc-presence?)
     :on-message [::room-presence-received]}
    ;; TODO: Don't open transport until first producer received
    ::fx/start-consuming nil}))

(rf/reg-event-fx
 ::room-presence-received
 (fn [{:keys [db]} [_ presence-stanza]]
   (let [occupant-jid (get-in presence-stanza [:attrs :from])]
     (if-let [producers (presence->producers presence-stanza)]
       {:db (assoc-in db [:media/producers occupant-jid] producers)}
       {:db (update db :media/producers dissoc occupant-jid)}))))

;; BROADCASTING
;; ============

(rf/reg-event-fx
 ::start-broadcast-button-clicked
 (fn [_ _]
   {::fx/request-user-media
    {:on-ready [::user-media-ready]
     :on-error [::error-occurred "Could not access webcam and microphone."]}}))

(rf/reg-event-fx
 ::user-media-ready
 (fn [_ _]
   {::fx/start-broadcasting {:on-ready [::broadcasting-ready]}}))

(rf/reg-event-fx
 ::broadcasting-ready
 (fn [{:keys [db]} [_ {:keys [video-producer-id audio-producer-id]}]]
   (let [{:connection/keys [full-jid]
          :chats/keys [chats active-chat-jid]}
         db]
     {:db (assoc db
                 :media/broadcasting? true
                 :media/video-producer-id video-producer-id
                 :media/audio-producer-id audio-producer-id)
      :xmpp/send
      {:stanza (media-presence-stanza
                {:from full-jid
                 :to (get-in chats [active-chat-jid :chat/from-jid])
                 :video-producer-id video-producer-id
                 :audio-producer-id audio-producer-id})}})))
