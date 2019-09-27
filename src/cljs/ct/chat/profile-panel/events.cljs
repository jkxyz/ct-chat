(ns ct.chat.profile-panel.events
  (:require
   [re-frame.core :as rf]))

(rf/reg-event-fx
 ::close-button-clicked
 (fn [{:keys [db]}]
   {:db (assoc db :profile-panel/open? false)}))

(rf/reg-event-fx
 ::action-selected
 (fn [{:keys [db]}]
   {}))
