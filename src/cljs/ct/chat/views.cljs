(ns ct.chat.views
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [ct.chat.xmpp.jids :refer [jidparts bare-jid]]
   [ct.chat.events :as events]
   [ct.chat.messages.events :as messages.events]
   [ct.chat.rooms.subscriptions :as rooms.subs]
   [ct.chat.messages.subscriptions :as messages.subs]
   [ct.chat.chats.subscriptions :as chats.subs]
   [ct.chat.media.subscriptions :as media.subs]
   [ct.chat.subscriptions :as subs]
   [ct.chat.profile-panel.views :refer [profile-panel]]
   [ct.chat.media.views
    :refer [broadcast-button
            webcam-preview
            webcams]]))

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
  (let [occupants (rf/subscribe [::rooms.subs/current-room-available-occupants-sorted])]
    (fn []
      [:div.roster-container
       (for [{:occupant/keys [occupant-jid] :as occupant} @occupants]
         ^{:key occupant-jid} [roster-user occupant])])))

(defn- sidebar []
  (let [broadcasting? (rf/subscribe [::media.subs/broadcasting?])]
    (fn []
      [:div.sidebar-container
       [:div.broadcast-container
        (when @broadcasting?
          [webcam-preview])
        [broadcast-button]]
       [roster]])))

(defn- occupant-name [{:occupant/keys [username nickname]}] (or username nickname))

(defn- status-message-body
  [{:message/keys [actor-occupant-jid occupant-jid]}]
  (let [actor-occupant (rf/subscribe [::rooms.subs/occupant
                                      (bare-jid actor-occupant-jid)
                                      actor-occupant-jid])
        occupant (rf/subscribe [::rooms.subs/occupant
                                (bare-jid occupant-jid)
                                occupant-jid])]
    (fn [{:message/keys [action]}]
      (let [actor-name (occupant-name @actor-occupant)
            name (occupant-name @occupant)]
        [:span.message-status-body
         (case action
           :kicked (str name " was kicked by " actor-name)
           :banned (str name " was banned"))]))))

(defn- message-container
  [{:message/keys [type from-username from-nickname body] :as message}]
  [:div.message-container
   (when (= :message type)
     [:<> [:span.message-from (or from-username from-nickname)] ": "])
   (case type
     :message [:span.message-body body]
     :status [status-message-body message])])

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
          (for [{:message/keys [id] :as message} @messages]
            ^{:key id} [message-container message])]])})))

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
         (case type
           ;; TODO: Use username for private message title when available
           :chat (:resource (jidparts jid))
           :groupchat (:room/name @room))
         (when (< 0 unread-messages-count) [:span " (" unread-messages-count ")"])]))))

(defn chat-tabs []
  (let [chats (rf/subscribe [::chats.subs/chats])
        active-chat-jid (rf/subscribe [::chats.subs/active-chat-jid])]
    (fn []
      [:div.chat-tabs-container
       (doall
        (for [{:chat/keys [jid] :as chat} @chats]
          ^{:key jid} [chat-tab {:chat chat :active? (= @active-chat-jid jid)}]))])))

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
