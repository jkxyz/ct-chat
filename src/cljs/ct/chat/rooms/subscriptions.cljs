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
 ::current-room-available-occupants
 :<- [::current-room-occupants]
 (fn [occupants]
   (filter #(= :available (:occupant/presence %)) occupants)))

(def ^:private role-rankings {:moderator 1 :participant 2 :visitor 3})

(def ^:private affiliation-rankings {:owner 1 :admin 2 :member 3 :none 4})

(rf/reg-sub
 ::current-room-available-occupants-sorted
 :<= [::current-room-available-occupants]
 (fn [occupants]
   (->> occupants
        (sort-by (some-fn :occupant/username :occupant/nickname))
        (sort-by #(get role-rankings (:occupant/role %) 99))
        (sort-by #(get affiliation-rankings (:occupant/affiliation %) 99)))))

(rf/reg-sub
 ::all-rooms
 (fn [db _]
   (:rooms/rooms db)))

(rf/reg-sub
 ::room
 :<- [::all-rooms]
 (fn [all-rooms [_ room-jid]]
   (get all-rooms room-jid)))

(rf/reg-sub
 ::occupant
 :<- [::all-occupants]
 (fn [occupants [_ room-jid occupant-jid]]
   (get-in occupants [room-jid occupant-jid])))
