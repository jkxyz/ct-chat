(ns ct.chat.messages.events
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [ct.chat.xmpp.jids :refer [bare-jid jidparts]]
   [ct.chat.xmpp.stanzas.message :refer [message-stanza? message]]
   [ct.chat.xmpp.stanzas.muc :refer [muc-message?]]))

(rf/reg-event-fx
 ::initialize
 (fn [_ _]
   {:xmpp/add-listener
    {:id ::messages-listener
     :xform (filter message-stanza?)
     :on-message [::message-received]}}))

(defn- get-self-occupant-jid [db room-jid]
  (:occupant/occupant-jid (first (filter :self? (vals (get-in db [:rooms/occupants room-jid]))))))

(defn- message-with-from-names [message app-db]
  (let [{:message/keys [from-jid chat-jid from-occupant-jid]} message
        room-jid (bare-jid from-occupant-jid)]
    (if from-jid
      (let [{username :local nickname :resource} (jidparts from-jid)]
        (assoc message
               :message/from-username username
               :message/from-nickname nickname))
      (let [from-occupant (get-in app-db [:rooms/occupants room-jid from-occupant-jid])]
        (assoc message
               :message/from-username (:occupant/username from-occupant)
               :message/from-nickname (:occupant/nickname from-occupant))))))

(def ^:const max-messages 1000)

(defn- append-message [app-db message]
  (let [{:message/keys [chat-jid]} message]
    (update-in app-db
               [:messages/messages chat-jid]
               (comp vec (partial take max-messages) conj)
               message)))

(defn- ensure-chat-exists [app-db message]
  (let [{:message/keys [message-type chat-jid room-jid]} message]
    (if-not (get-in app-db [:chats/chats chat-jid])
      (assoc-in app-db
                [:chats/chats chat-jid]
                {:chat/jid chat-jid
                 :char/type message-type
                 :chat/from-jid (get-self-occupant-jid app-db chat-jid)})
      app-db)))

(defn- increment-unread-messages [app-db message]
  (let [{:chats/keys [active-chat-jid]} app-db
        {:message/keys [chat-jid]} message]
    (if-not (= active-chat-jid chat-jid)
      (update-in app-db [:chats/chats chat-jid :chat/unread-messages-count] inc)
      app-db)))

(rf/reg-event-fx
 ::message-received
 (fn [{:keys [db]} [_ message-stanza]]
   (let [message (-> (message message-stanza) (message-with-from-names db))]
     {:db (-> db
              (append-message message)
              (ensure-chat-exists message)
              (increment-unread-messages message))})))
