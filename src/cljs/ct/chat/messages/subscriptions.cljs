(ns ct.chat.messages.subscriptions
  (:require
   [re-frame.core :as rf]
   [ct.chat.chats.subscriptions :as chats.subs]))

(rf/reg-sub
 ::all-messages
 (fn [db]
   (:messages/messages db)))

(rf/reg-sub
 ::current-room-messages
 :<- [::chats.subs/active-chat-jid]
 :<- [::all-messages]
 (fn [[active-chat-jid all-messages]]
   (get all-messages active-chat-jid)))
