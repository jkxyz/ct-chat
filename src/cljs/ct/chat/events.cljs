(ns ct.chat.events
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [ct.chat.xmpp.stanzas.muc :refer [muc-message-stanza]]
   [ct.chat.xmpp.stanzas.message :refer [message-stanza message]]
   [ct.chat.xmpp.stanzas.presence :refer [presence-stanza]]
   [ct.chat.db :refer [initial-db]]
   [ct.chat.connection.events :as connection.events]
   [ct.chat.rooms.events :as rooms.events]
   [ct.chat.messages.events :as messages.events]
   [ct.chat.media.events :as media.events]))

(rf/reg-event-fx
 ::initialize
 (fn [_ [_ config]]
   {:db (initial-db config)
    :dispatch-n
    [[::connection.events/initialize {:on-ready [::rooms.events/initialize]}]
     [::messages.events/initialize]
     [::media.events/initialize]]}))

(comment
  (let [{:keys [from-jid]} (get-in db [:chats/chats (:chats/active-chat-jid db)])]
    {:db (-> db
             (assoc-in [:chats/chats occupant-jid] {:jid occupant-jid
                                                    :type :chat
                                                    :from-jid from-jid})
             (assoc :chats/active-chat-jid occupant-jid))}))

(rf/reg-event-fx
 ::roster-user-clicked
 (fn [{:keys [db]} [_ {:keys [occupant-jid]}]]
   (let [{:chats/keys [active-chat-jid]
          :profile-panel/keys [open?]
          previous-occupant-jid :profile-panel/occupant-jid}
         db]
     {:db (assoc db
                 :profile-panel/open?
                 (or (not open?) (not= previous-occupant-jid occupant-jid))
                 :profile-panel/chat-jid active-chat-jid
                 :profile-panel/occupant-jid occupant-jid)})))

(rf/reg-event-fx
 ::profile-panel-close-button-clicked
 (fn [{:keys [db]} _]
   {:db (assoc db :profile-panel/open? false)}))

(rf/reg-event-fx
 ::chat-tab-clicked
 (fn [{:keys [db]} [_ {:keys [jid]}]]
   {:db (-> db
            (assoc :chats/active-chat-jid jid)
            (assoc-in [:chats/chats jid :unread-messages-count] 0))}))

(rf/reg-event-fx
 ::message-input-submitted
 (fn [{:keys [db]} [_ message-text]]
   (let [{:connection/keys [full-jid] :chats/keys [active-chat-jid]} db
         active-chat (get-in db [:chats/chats active-chat-jid])
         message-attrs {:from full-jid
                        :to active-chat-jid
                        :body message-text}
         stanza (condp = (:type active-chat)
                  :chat (message-stanza message-attrs)
                  :groupchat (muc-message-stanza message-attrs))]
     (cond-> {:xmpp/send {:stanza stanza}}
       (= :chat (:type active-chat))
       (merge
        {:db (update-in db
                        [:messages/messages active-chat-jid]
                        (comp vec conj)
                        (assoc (message stanza) :from (:from-jid active-chat)))})))))

(rf/reg-event-fx
 ::broadcast-button-clicked
 (fn [{:keys [db]} _]
   (let [{:chats/keys [active-chat-jid]} db]
     {:dispatch [::media.events/broadcast-requested]})))

(rf/reg-event-fx
 ::stop-broadcasting-button-clicked
 (fn [{:keys [db]} _]
   (let [{:connection/keys [full-jid]
          :chats/keys [chats active-chat-jid]}
         db]
     {:xmpp/send
      {:stanza
       (presence-stanza
        {:from full-jid
         :to (get-in chats [active-chat-jid :from-jid])})}})))
