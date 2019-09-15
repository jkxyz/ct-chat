(ns ct.chat.rooms.events
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [ct.chat.jids :refer [bare-jid]]
   [ct.chat.xmpp.stanzas.disco :refer [disco-info-query-content]]
   [ct.chat.xmpp.stanzas.muc
    :refer [muc-room-presence-stanza
            muc-self-presence?
            muc-presence?
            muc-presence->occupant
            iq-result->room-info]]))

(rf/reg-event-fx
 ::rooms-error-occurred
 (fn [_ [_ message]]
   (js/window.alert (str "Error! " message))
   {}))

(rf/reg-event-fx
 ::initialize
 (fn [{:keys [db]} _]
   {:db (assoc db :chats/active-chat-jid (:rooms/default-room-jid db))
    :dispatch [::presence-listeners-ready]
    :xmpp/add-listener
    {:id ::room-presence-listener
     :xform (filter muc-presence?)
     :on-message [::room-presence-received]}}))

(rf/reg-event-fx
 ::room-presence-received
 (fn [{:keys [db]} [_ presence-stanza]]
   (let [{:occupant/keys [room-jid occupant-jid] :as occupant}
         (muc-presence->occupant presence-stanza)]
     {:db (assoc-in db [:rooms/occupants room-jid occupant-jid] occupant)})))

(rf/reg-event-fx
 ::presence-listeners-ready
 (fn [{:keys [db]} _]
   (let [{:connection/keys [full-jid] :rooms/keys [default-room-jid]} db]
     {:xmpp/query
      {:type :get
       :from full-jid
       :to default-room-jid
       :content (disco-info-query-content)
       :on-result [::default-room-info-received]
       :on-error [::rooms-error-occurred "Could not join default room."]}})))

(rf/reg-event-fx
 ::default-room-info-received
 (fn [{:keys [db]} [_ iq-result-stanza]]
   (let [{:rooms/keys [default-room-jid default-nickname]
          :connection/keys [full-jid]} db
         occupant-jid (str default-room-jid "/" default-nickname)
         room-info (iq-result->room-info iq-result-stanza)]
     {:xmpp/send-then-listen
      {:stanza (muc-room-presence-stanza {:from full-jid :to occupant-jid})
       :on-message [::room-self-presence-received]
       :on-timeout [::rooms-error-occurred "Could not join default room."]
       :xform (filter muc-self-presence?)}
      :db (assoc-in db [:rooms/rooms (:room/jid room-info)] room-info)})))

(rf/reg-event-fx
 ::room-self-presence-received
 (fn [{:keys [db]} [_ presence-stanza]]
   (let [occupant-jid (get-in presence-stanza [:attrs :from])
         room-jid (first (string/split occupant-jid "/"))]
     {:db (assoc-in db [:chats/chats room-jid] {:jid room-jid
                                                :type :groupchat
                                                :from-jid occupant-jid})})))
