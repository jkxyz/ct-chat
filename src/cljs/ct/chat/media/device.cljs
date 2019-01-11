(ns ct.chat.media.device
  (:require
   [cljs.core.async :refer [chan close! go <! put!]]
   [mediasoup-client :as mediasoup]
   [ct.chat.media.signalling.peer :as peer]))

(defonce ^:private device (delay (mediasoup/Device.)))

(defonce ^:private receive-transport (atom nil))

(defonce ^:private send-transport (atom nil))

(defonce ^:private user-media-stream (atom nil))

(defonce consumers (atom {}))

(defn load-device! [capabilities]
  (let [ch (chan)]
    (-> (.load @device (clj->js {:routerRtpCapabilities capabilities}))
        (.then #(close! ch)))
    ch))

(defn create-receive-transport! [parameters]
  (let [transport (.createRecvTransport @device (clj->js parameters))]
    (.on transport "connect"
         (fn [data callback errback]
           (go
             (<! (peer/request! :connect-receive-transport data))
             (callback))))
    (reset! receive-transport transport)))

(defn create-send-transport! [parameters]
  (let [transport (.createSendTransport @device (clj->js parameters))]
    (.on transport "connect"
         (fn [data callback errback]
           (go
             (<! (peer/request! :connect-send-transport data))
             (callback))))
    (.on transport "produce"
         (fn [parameters callback errback]
           (go
             (let [response (<! (peer/request! :create-producer parameters))]
               (callback (clj->js {:id (.-videoProducer.id response)}))))))
    (reset! send-transport transport)))

(defn request-user-media! []
  (let [ch (chan)]
    (-> (js/window.navigator.mediaDevices.getUserMedia #js {:video true :audio true})
        (.then
         (fn [media-stream]
           (reset! user-media-stream media-stream)
           (put! ch :ok)))
        (.catch
         (fn [error]
           (js/console.error error)
           (close! ch))))
    ch))

(defn broadcast-webcam! []
  (let [ch (chan)]
    (let [track (get (.getVideoTracks @user-media-stream) 0)]
      (-> (.produce
           @send-transport
           (clj->js
            {:track track
             :encodings [{:maxBitrate 1000}]
             :codecOptions {:videoGoogleStartBitrate 1000}}))
          (.then
           (fn [producer]
             (put! ch (.-id producer))))))
    ch))

(defn create-consumer! [parameters {:keys [on-track-ended]}]
  (let [ch (chan)]
    (-> (.consume @receive-transport parameters)
        (.then
         (fn [consumer]
           (.on consumer "trackended" on-track-ended)
           (swap! consumers assoc (.-id consumer) consumer)
           (close! ch))))
    ch))
