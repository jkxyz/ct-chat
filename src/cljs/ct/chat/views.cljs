(ns ct.chat.views
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [ct.chat.media.device :refer [consumers]]
   [ct.chat.events :as events]
   [ct.chat.messages.events :as messages.events]
   [ct.chat.rooms.subscriptions :as rooms.subs]
   [ct.chat.messages.subscriptions :as messages.subs]
   [ct.chat.chats.subscriptions :as chats.subs]
   [ct.chat.media.subscriptions :as media.subs]
   [ct.chat.subscriptions :as subs]))

(defn- input []
  (let [!input (atom nil)
        handle-submit
        (fn [e]
          (rf/dispatch [::events/message-input-submitted (.-value @!input)])
          (set! (.-value @!input) "")
          (.preventDefault e))]
    (fn []
      [:form.input-container
       {:on-submit handle-submit}
       [:input.input
        {:ref (partial reset! !input)
         :type "text"
         :placeholder "Type a message"}]])))

(defn- rooms []
  [:div.rooms-container
   (let [rooms ["Test"]]
     (if (seq rooms)
       (for [room rooms]
         [:div.rooms-room {:key room} room])
       [:div.rooms-room "Loading rooms..."]))])

(def crown-emoji "👑")

(defn- roster-user [{:keys [occupant-jid affiliation]}]
  (let [handle-click
        #(rf/dispatch [::events/roster-user-clicked {:occupant-jid occupant-jid}])]
    (fn [{:keys [occupant-jid username nickname]}]
      [:div.roster-user-container {:key occupant-jid :on-click handle-click}
       [:div.roster-icon]
       [:div.roster-jid (or username nickname)]
       [:div.roster-affiliation {:title (string/capitalize (name affiliation))}
        (case affiliation
          :owner crown-emoji
          "")]])))

(defn- roster []
  (let [occupants (rf/subscribe [::rooms.subs/current-room-occupants])]
    (fn []
      [:div.roster-container
       (for [{:keys [occupant-jid] :as occupant} @occupants]
         ^{:key occupant-jid} [roster-user occupant])])))

(defn- broadcast-button [_]
  (let [handle-click #(rf/dispatch [::events/broadcast-button-clicked])]
    (fn [{:keys [disabled?]}]
      [:button.broadcast-button {:disabled disabled? :on-click handle-click}
       "Broadcast"])))

(defn- stop-broadcasting-button []
  (let [handle-click #(rf/dispatch [::events/stop-broadcasting-button-clicked])]
    (fn []
      [:button.stop-broadcasting-button {:on-click handle-click}
       "Stop Broadcasting"])))

(defn- broadcast []
  (let [broadcast-state (rf/subscribe [::media.subs/broadcast-state])]
    (fn []
      [:div.broadcast-container
       (if (or (nil? @broadcast-state) (= :preparing @broadcast-state))
         [broadcast-button {:disabled? (= :preparing @broadcast-state)}]
         [stop-broadcasting-button])])))

(defn- sidebar []
  [:div.sidebar-container
   #_[rooms]
   [broadcast]
   [roster]])

(defn- autoscrolling-messages []
  (let [!container (atom nil)
        scroll (fn [] (.scrollTo @!container 0 (.-scrollHeight @!container)))
        messages (rf/subscribe [::messages.subs/current-room-messages])]
    (reagent/create-class
     {:display-name "autoscrolling-messages"
      :component-did-mount scroll
      :component-did-update scroll
      :reagent-render
      (fn []
        [:div.messages-container {:ref (partial reset! !container)}
         [:div.messages-scroll-container
          (for [{:keys [id from-username from-nickname body]} @messages]
            [:div.message-container {:key id}
             [:span.message-from
              (or from-username from-nickname)] ": "
             [:span.message-body body]])]])})))

(defn nickname [occupant-jid] (last (clojure.string/split occupant-jid "/")))

(defn chat-tab [{:keys [jid]}]
  (let [handle-click #(rf/dispatch [::events/chat-tab-clicked {:jid jid}])
        room (rf/subscribe [::rooms.subs/room jid])]
    (fn [{:keys [active? jid type unread-messages-count]}]
      [:a.chat-tab
       {:class [(when active? "active")]
        :on-click handle-click}
       (condp = type
         ;; TODO: Put room title in db
         :chat (nickname jid)
         :groupchat (:name @room))
       (when (< 0 unread-messages-count)
         [:span " (" unread-messages-count ")"])])))

(defn chat-tabs []
  (let [chats (rf/subscribe [::chats.subs/chats])
        active-chat-jid (rf/subscribe [::chats.subs/active-chat-jid])]
    (fn []
      [:div.chat-tabs-container
       (doall
        (for [{:keys [jid] :as chat} @chats]
          ^{:key jid} [chat-tab (merge chat {:active? (= @active-chat-jid jid)})]))])))

(defn webcam [{:keys [consumer-id]}]
  (let [!video (atom nil)]
    (reagent/create-class
     {:display-name "webcam"
      :component-did-mount
      (fn []
        (let [consumer (@consumers consumer-id)]
          (set! (.-srcObject @!video) (js/window.MediaStream. #js [(.-track consumer)]))
          (set! (.-volume @!video) 0)
          (.play @!video)))
      :reagent-render
      (fn []
        [:div.webcam
         [:video.webcam-video {:ref (partial reset! !video)}]])})))

(defn webcams []
  (let [consumer-ids (rf/subscribe [::media.subs/consumer-ids])]
    (fn []
      [:div.webcams-container
       (for [consumer-id @consumer-ids]
         ^{:key consumer-id} [webcam {:consumer-id consumer-id}])])))

(defn profile-actions [{:keys [actions on-action]}]
  (let [handle-input
        (fn [event]
          (on-action (keyword (.-target.value event)))
          (set! (.-target.value event) ""))]
    (fn []
      [:select.profile-actions
       {:default-value ""
        :on-input handle-input}
       [:option {:disabled true :value ""}
        "Choose an action..."]
       (for [[type title] actions]
         ^{:key type}
         [:option {:value (name type)}
          title])])))

(defn profile-panel []
  (let [profile-panel (rf/subscribe [::subs/profile-panel])
        handle-close-button-click
        #(rf/dispatch [::events/profile-panel-close-button-clicked])]
    (fn []
      (when-let [{:keys [nickname affiliation role]} @profile-panel]
        [:div.profile-container
         [:div.profile-close-button {:on-click handle-close-button-click}]
         [:div.profile-image-container
          [:div.profile-image]]
         [:a.profile-link-button
          "View Profile"]
         [:a.profile-link-button
          "Send Message"]
         [:div.profile-details
          (when-not (= :none affiliation)
            [:div.profile-detail
             [:div.profile-detail-title "Affiliation"]
             [:div.profile-detail-value (string/capitalize (name affiliation))]])
          [:div.profile-detail
           [:div.profile-detail-title "Role"]
           [:div.profile-detail-value (string/capitalize (name role))]]]
         [:div.profile-actions-container
          [profile-actions
           {:on-action #(rf/dispatch [::events/profile-action-selected %])
            :actions [[:ban "Ban this user"]]}]]]))))

(defn app []
  (let [active-chat (rf/subscribe [::chats.subs/active-chat])]
    (fn []
      (if-not @active-chat
        [:div.loader-container [:div.loader "Loading..."]]
        [:div.chat-container
         [webcams]
         [chat-tabs]
         [:div.messages-and-roster-container
          [autoscrolling-messages]
          [profile-panel]
          (when (= :groupchat (:type @active-chat))
            [sidebar])]
         [input]]))))
