(ns ct.chat.rooms.subscriptions
  (:require
   [re-frame.core :as rf]
   [ct.chat.chats.subscriptions :as chats.subs]))

(rf/reg-sub
 ::all-occupants
 (fn [db _]
   (:rooms/occupants db)))

(rf/reg-sub
 ::current-room-occupants
 :<- [::all-occupants]
 :<- [::chats.subs/active-chat-jid]
 (fn [[occupants active-chat-jid] _]
   (vals (get occupants active-chat-jid))))

(rf/reg-sub
 ::all-rooms
 (fn [db _]
   (:rooms/rooms db)))

(rf/reg-sub
 ::room
 :<- [::all-rooms]
 (fn [all-rooms [_ room-jid]]
   (get all-rooms room-jid)))
