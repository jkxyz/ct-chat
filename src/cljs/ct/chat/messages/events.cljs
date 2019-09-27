(ns ct.chat.messages.events
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [ct.chat.xmpp.jids :refer [bare-jid jidparts]]
   [ct.chat.xmpp.stanzas.message :refer [message-stanza? message]]
   [ct.chat.xmpp.stanzas.muc :refer [muc-message?]]))

(def ^:const max-messages 1000)

(rf/reg-event-fx
 ::initialize
 (fn [_ _]
   {:xmpp/add-listener
    {:id ::messages-listener
     :xform (filter message-stanza?)
     :on-message [::message-received]}}))

(defn- get-self-occupant-jid [db room-jid]
  (:occupant/occupant-jid (first (filter :self? (vals (get-in db [:rooms/occupants room-jid]))))))

(rf/reg-event-fx
 ::message-received
 (fn [{:keys [db]} [_ message-stanza]]
   (let [{:chats/keys [active-chat-jid]} db
         {:message/keys [chat-jid from-jid from-occupant-jid] :as message}
         (message message-stanza)
         room-jid (bare-jid from-occupant-jid)]
     {:db (cond-> db
            :always
            (update-in [:messages/messages chat-jid]
                       (comp vec (partial take max-messages) conj)
                       (if from-jid
                         (assoc message
                                :message/from-username (:local (jidparts from-jid))
                                :message/from-nickname (:resource (jidparts from-jid)))
                         (assoc message
                                :message/from-username (get-in db
                                                               [:rooms/occupants
                                                                room-jid
                                                                from-occupant-jid
                                                                :occupant/username])
                                :message/from-nickname (get-in db
                                                               [:rooms/occupants
                                                                room-jid
                                                                from-occupant-jid
                                                                :occupant/nickname]))))
            (not (get-in db [:chats/chats chat-jid]))
            (assoc-in [:chats/chats chat-jid]
                      {:chat/jid chat-jid
                       :char/type (if (muc-message? message-stanza) :groupchat :chat)
                       :chat/from-jid (get-self-occupant-jid db room-jid)})
            (not= active-chat-jid chat-jid)
            (update-in [:chats/chats chat-jid :chat/unread-messages-count] inc))})))
