(ns ct.chat.media.views
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as rf]
   [ct.chat.media.events :as events]
   [ct.chat.media.subscriptions :as media.subs]
   [ct.chat.media.device :refer [user-media-stream]]))

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
