(ns ct.chat.media.views
  (:require
   [cljs.core.async :refer [go <!]]
   [reagent.core :as reagent]
   [re-frame.core :as rf]
   [ct.chat.media.events :as events]
   [ct.chat.media.subscriptions :as media.subs]
   [ct.chat.media.device
    :refer [user-media-stream
            consume!]]))

(defn- start-broadcast-button []
  (let [handle-click #(rf/dispatch [::events/start-broadcast-button-clicked])]
    (fn []
      [:button.broadcast-button {:on-click handle-click} "Broadcast"])))

(defn- stop-broadcast-button []
  (let [handle-click #(rf/dispatch [::events/stop-broadcast-button-clicked])]
    (fn []
      [:button.broadcast-button {:on-click handle-click} "Stop Broadcasting"])))

(defn broadcast-button []
  (let [broadcasting? (rf/subscribe [::media.subs/broadcasting?])]
    (fn []
      (if @broadcasting?
        [stop-broadcast-button]
        [start-broadcast-button]))))

(defn- remove-audio-tracks [media-stream]
  (let [media-stream (.clone media-stream)]
    (run! #(.removeTrack media-stream %) (.getAudioTracks media-stream))
    media-stream))

(defn webcam-preview []
  (let [broadcasting? (rf/subscribe [::media.subs/broadcasting?])
        video (atom nil)
        display-video!
        (fn []
          (when @user-media-stream
            (let [video-stream (remove-audio-tracks @user-media-stream)]
              (set! (.-srcObject @video) video-stream)
              (set! (.-volume @video) 0)
              (.play @video))))]
    (reagent/create-class
     {:component-did-mount (fn [] (display-video!))
      :component-did-update (fn [] (display-video!))
      :reagent-render
      (fn []
        (when @broadcasting?
          [:div.webcam-preview-container
           [:video.webcam-preview-video {:ref #(reset! video %)}]]))})))

(defn- webcam [{:keys [producer-id]}]
  (let [video (atom nil)
        display-video!
        (fn []
          (go
            ;; TODO: Close the consumer
            (let [consumer (<! (consume! producer-id))
                  media-stream (js/window.MediaStream. (clj->js [(.-track consumer)]))]
              (set! (.-srcObject @video) media-stream)
              (set! (.-volume @video) 0)
              (.play @video))))]
    (reagent/create-class
     {:component-did-mount (fn [] (display-video!))
      :reagent-render
      (fn [_]
        [:div.webcam-container
         [:video.webcam-video {:ref #(reset! video %)}]])})))

(defn webcams []
  (let [video-producers (rf/subscribe [::media.subs/video-producers])]
    (fn []
      [:div.webcams-container
       (for [{:producer/keys [id]} @video-producers]
         ^{:key id} [webcam {:producer-id id}])])))
