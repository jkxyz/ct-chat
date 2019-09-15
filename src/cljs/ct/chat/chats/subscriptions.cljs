(ns ct.chat.chats.subscriptions
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 ::chats
 (fn [db]
   (vals (:chats/chats db))))

(rf/reg-sub
 ::active-chat-jid
 (fn [db]
   (:chats/active-chat-jid db)))

(rf/reg-sub
 ::active-chat
 :<- [::chats]
 :<- [::active-chat-jid]
 (fn [[chats active-chat-jid] _]
   (first (filter #(= active-chat-jid (:chat/jid %)) chats))))
