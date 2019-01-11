(ns ct.chat.subscriptions
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 ::profile
 (fn [{:profile/keys [open? chat-jid occupant-jid]}]
   (when open?
     {:chat-jid chat-jid
      :occupant-jid occupant-jid})))
