(ns ct.chat.media.events
  (:require
   [re-frame.core :as rf]
   [day8.re-frame.http-fx]
   [ajax.core :as ajax]
   [ct.chat.xmpp.stanzas.muc
    :refer [muc-presence?
            muc-self-presence?]]
   [ct.chat.xmpp.stanzas.media
    :refer [presence->producers
            media-presence-stanza]]
   [ct.chat.media.effects :as fx]))

(rf/reg-event-fx
 ::initialize
 (fn [{:keys [db]} _]
   {:db (assoc db :media/signalling-connected? false)
    ::fx/connect {:url (:media/signalling-websocket-uri db)}
    :xmpp/add-listener
    {:id ::room-presence-listener
     :xform (filter muc-presence?)
     :on-message [::room-presence-received]}}))

(rf/reg-event-fx
 ::room-presence-received
 (fn [{:keys [db]} [_ presence-stanza]]
   (when-not (muc-self-presence? presence-stanza)
     (let [occupant-jid (get-in presence-stanza [:attrs :from])]
       (if-let [producers (presence->producers presence-stanza)]
         {:db (assoc-in db [:media/producers occupant-jid] producers)
          ::fx/start-consuming nil}
         {:db (update db :media/producers dissoc occupant-jid)})))))

;; Broadcasting
;; ============

(rf/reg-event-fx
 ::start-broadcast-button-clicked
 (fn [{:keys [db]} _]
   {:db (assoc db :media/broadcast-chat-jid (:chats/active-chat-jid db))
    ::fx/request-user-media
    {:on-ready [::user-media-ready]
     :on-error [::user-media-error-occurred]}}))

(rf/reg-event-fx
 ::user-media-ready
 (fn [{:keys [db]} _]
   (if-not (:media/signalling-connected? db)
     {::fx/connect
      {:url (:media/signalling-websocket-uri db)
       :on-open [::connection-ready-for-broadcast]}}
     {:dispatch [::connection-ready-for-broadcast]})))

(rf/reg-event-fx
 ::user-media-error-occurred
 (fn [_ _]
   nil))

(rf/reg-event-fx
 ::connection-ready-for-broadcast
 (fn [{:keys [db]} _]
   {:db (assoc db :media/signalling-connected? true)
    ::fx/start-broadcasting {:on-ready [::broadcasting-ready]}}))

(rf/reg-event-fx
 ::broadcasting-ready
 (fn [{:keys [db]} [_ {:keys [video-producer-id audio-producer-id]}]]
   (let [{:connection/keys [full-jid] :media/keys [broadcast-chat-jid]} db]
     {:db (assoc db
                 :media/broadcasting? true
                 :media/video-producer-id video-producer-id
                 :media/audio-producer-id audio-producer-id)
      :xmpp/send {:stanza (media-presence-stanza
                           {:from full-jid
                            :to broadcast-chat-jid
                            :video-producer-id video-producer-id
                            :audio-producer-id audio-producer-id})}})))
