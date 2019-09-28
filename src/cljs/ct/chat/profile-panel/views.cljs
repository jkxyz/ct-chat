(ns ct.chat.profile-panel.views
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [ct.chat.profile-panel.subscriptions :as subs]
   [ct.chat.profile-panel.events :as events]))

(defn- profile-actions [{:keys [on-action]}]
  (let [handle-input
        (fn [event]
          (on-action (keyword (.-target.value event)))
          (set! (.-target.value event) ""))]
    (fn [{:keys [actions]}]
      [:select.profile-actions
       {:default-value ""
        :on-input handle-input}
       [:option {:disabled true :value ""}
        "Choose an action..."]
       (for [[type title] actions]
         ^{:key type}
         [:option {:value (name type)}
          title])])))

(def ^:private action-titles
  {:kick "Kick"
   :grant-voice "Grant voice"
   :revoke-voice "Revoke voice"
   :ban "Ban"
   :assign-moderator-role "Assign moderator role"
   :revoke-moderator-role "Revoke moderator role"})

(defn profile-panel []
  (let [occupant (rf/subscribe [::subs/occupant])
        available-actions (rf/subscribe [::subs/available-actions])
        handle-close-button-click #(rf/dispatch [::events/close-button-clicked])]
    (fn []
      (when-let [{:occupant/keys [nickname affiliation role]} @occupant]
        [:div.profile-container
         [:div.profile-close-button {:on-click handle-close-button-click}]
         [:div.profile-image-container
          [:div.profile-image]]
         [:a.profile-link-button
          "View Profile"]
         [:a.profile-link-button
          "Send Message"]
         (when (and (not= :none affiliation) (not= :participant role))
           [:div.profile-details
            (when-not (= :none affiliation)
              [:div.profile-detail
               [:div.profile-detail-title "Affiliation"]
               [:div.profile-detail-value (string/capitalize (name affiliation))]])
            (when-not (= :participant role)
              [:div.profile-detail
               [:div.profile-detail-title "Role"]
               [:div.profile-detail-value (string/capitalize (name role))]])])
         (when @available-actions
           [:div.profile-actions-container
            [profile-actions
             {:on-action #(rf/dispatch [::events/action-selected %])
              :actions (map (juxt identity action-titles) @available-actions)}]])]))))
