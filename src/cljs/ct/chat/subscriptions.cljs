(ns ct.chat.subscriptions
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 ::profile-panel
 (fn [{:profile-panel/keys [open? chat-jid occupant-jid]
       :rooms/keys [occupants]}]
   (when open?
     (let [{:keys [nickname affiliation role]}
           (get-in occupants [chat-jid occupant-jid])]
       {:nickname nickname
        :affiliation affiliation
        :role role}))))
