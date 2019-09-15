(ns ct.chat.messages.events
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
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
  (:occupant-jid (first (filter :self? (vals (get-in db [:rooms/occupants room-jid]))))))

(defn- jid-localpart [jid] (first (string/split jid "@")))

(defn- nickname [occupant-jid] (last (string/split occupant-jid "/")))

(rf/reg-event-fx
 ::message-received
 (fn [{:keys [db]} [_ message-stanza]]
   (let [{:chats/keys [active-chat-jid]} db
         {:keys [chat-jid from-jid from-occupant-jid] :as message}
         (message message-stanza)
         room-jid (first (string/split from-occupant-jid "/"))]
     {:db (cond-> db
            :always
            (update-in [:messages/messages chat-jid]
                       (comp vec (partial take max-messages) conj)
                       (if from-jid
                         (assoc message
                                :from-username (jid-localpart from-jid)
                                :from-nickname (nickname from-occupant-jid))
                         (assoc message
                                :from-username (get-in db
                                                       [:rooms/occupants
                                                        room-jid
                                                        from-occupant-jid
                                                        :username])
                                :from-nickname (get-in db
                                                       [:rooms/occupants
                                                        room-jid
                                                        from-occupant-jid
                                                        :nickname]))))
            (not (get-in db [:chats/chats chat-jid]))
            (assoc-in [:chats/chats chat-jid]
                      {:jid chat-jid
                       :type (if (muc-message? message-stanza) :groupchat :chat)
                       :from-jid (get-self-occupant-jid db room-jid)})
            (not= active-chat-jid chat-jid)
            (update-in [:chats/chats chat-jid :chat/unread-messages-count] inc))})))
