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
   [ct.chat.subscriptions :as subs]
   [ct.chat.profile-panel.views :refer [profile-panel]]))

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

(defn- roster-user [{:occupant/keys [occupant-jid affiliation]}]
  (let [handle-click
        #(rf/dispatch [::events/roster-user-clicked {:occupant-jid occupant-jid}])]
    (fn [{:occupant/keys [occupant-jid username nickname]}]
      [:div.roster-user-container {:key occupant-jid :on-click handle-click}
       [:div.roster-icon]
       [:div.roster-jid (or username nickname)]
       [:div.roster-affiliation {:title (string/capitalize (name affiliation))}
        (case affiliation
          :owner crown-emoji
          "")]])))

(defn- roster []
  (let [occupants (rf/subscribe [::rooms.subs/current-room-occupants-sorted])]
    (fn []
      [:div.roster-container
       (for [{:occupant/keys [occupant-jid] :as occupant} @occupants]
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
          (for [{:message/keys [id from-username from-nickname body]} @messages]
            [:div.message-container {:key id}
             [:span.message-from
              (or from-username from-nickname)] ": "
             [:span.message-body body]])]])})))

(defn nickname [occupant-jid] (last (clojure.string/split occupant-jid "/")))

(defn chat-tab [{:keys [chat]}]
  (let [{:chat/keys [jid]} chat
        handle-click #(rf/dispatch [::events/chat-tab-clicked {:jid jid}])
        room (rf/subscribe [::rooms.subs/room jid])]
    (fn [{:keys [chat active?]}]
      (let [{:chat/keys [jid type unread-messages-count]} chat]
        [:a.chat-tab
         {:class [(when active? "active")]
          :on-click handle-click}
         (condp = type
           ;; TODO: Put room title in db
           :chat (nickname jid)
           :groupchat (:room/name @room))
         (when (< 0 unread-messages-count)
           [:span " (" unread-messages-count ")"])]))))

(defn chat-tabs []
  (let [chats (rf/subscribe [::chats.subs/chats])
        active-chat-jid (rf/subscribe [::chats.subs/active-chat-jid])]
    (fn []
      [:div.chat-tabs-container
       (doall
        (for [{:chat/keys [jid] :as chat} @chats]
          ^{:key jid} [chat-tab {:chat chat :active? (= @active-chat-jid jid)}]))])))

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
          (when (= :groupchat (:chat/type @active-chat))
            [sidebar])]
         [input]]))))
