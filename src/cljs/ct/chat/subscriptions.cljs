(ns ct.chat.subscriptions
  (:require
   [re-frame.core :as rf]
   [ct.chat.chats.subscriptions :as chats.subs]))

(rf/reg-sub
 ::profile-panel
 (fn [{:profile-panel/keys [open? chat-jid occupant-jid]
       :rooms/keys [occupants]}]
   (when open?
     (let [{:occupant/keys [nickname affiliation role]}
           (get-in occupants [chat-jid occupant-jid])]
       {:nickname nickname
        :affiliation affiliation
        :role role}))))

(rf/reg-sub
 ::available-profile-actions
 :<- [::chats.subs/active-chat-jid]
 (fn []))
