(ns ct.chat.media.views
  (:require
   [re-frame.core :as rf]
   [ct.chat.media.events :as events]))

(defn broadcast-button []
  (let [handle-click #(rf/dispatch [::events/broadcast-button-clicked])]
    (fn []
      [:button.broadcast-button {:on-click handle-click} "Broadcast"])))
