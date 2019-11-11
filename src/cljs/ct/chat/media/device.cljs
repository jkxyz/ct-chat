(ns ct.chat.media.device
  (:require
   [cljs.core.async :refer [chan close! go <! put!]]
   [ajax.core :as ajax]
   [mediasoup-client :as mediasoup]
   [ct.chat.media.connection :refer [request!]]))

(defonce user-media-stream (atom nil))

(def ^:private user-media-constraints #js {:video true :audio true})

(defn request-user-media! []
  (let [ch (chan)]
    (-> (js/navigator.mediaDevices.getUserMedia user-media-constraints)
        (.then
         (fn [media-stream]
           (reset! user-media-stream media-stream)
           (put! ch media-stream)))
        (.catch
         (fn [error]
           (js/console.error error)
           (close! ch))))
    ch))

(defonce ^:private device (atom nil))

(defn- load-device! [capabilities]
  (let [ch (chan)]
    (go
      (let [new-device (mediasoup/Device.)]
        (-> (.load new-device (clj->js {:routerRtpCapabilities capabilities}))
            (.then (fn [] (close! ch))))
        (reset! device new-device)))
    ch))

(defonce ^:private send-transport (atom nil))

(defn- create-send-transport! [parameters]
  (let [transport (.createSendTransport @device (clj->js parameters))]
    (doto transport
      (.on "connect"
           (fn [parameters callback errback]
             (go
               (if (<! (request! :connectTransport {:type "send" :parameters parameters}))
                 (callback)
                 (errback)))))
      (.on "produce"
           (fn [parameters callback errback]
             (go
               (if-let [p (<! (request! :createProducer {:parameters parameters}))]
                 (callback (clj->js p))
                 (errback))))))
    (reset! send-transport transport)))

(defn- produce-video! []
  (let [ch (chan)
        track (get (.getVideoTracks @user-media-stream) 0)]
    (-> (.produce @send-transport
                  (clj->js {:track track
                            :encodings [{:maxBitrate 1000}]
                            :codecOptions {:videoGoogleStartBitrate 1000}}))
        (.then #(put! ch %)))
    ch))

(defn- produce-audio! []
  (let [ch (chan)
        track (get (.getAudioTracks @user-media-stream) 0)]
    (-> (.produce @send-transport
                  (clj->js {:track track
                            :encodings [{:maxBitrate 1000}]}))
        (.then #(put! ch %)))
    ch))

(defonce ^:private video-producer (atom nil))

(defonce ^:private audio-producer (atom nil))

(defn start-broadcasting! []
  (go
    (when-not @device (<! (load-device! (<! (request! :capabilities)))))
    (create-send-transport! (<! (request! :createTransport {:type "send"})))
    (let [new-video-producer (<! (produce-video!))
          new-audio-producer (<! (produce-audio!))]
      (reset! video-producer new-video-producer)
      (reset! audio-producer new-audio-producer)
      {:video-producer-id (.-id new-video-producer)
       :audio-producer-id (.-id new-audio-producer)})))

(defonce ^:private receive-transport (atom nil))

(defn- create-receive-transport! [parameters]
  (let [transport (.createRecvTransport @device (clj->js parameters))]
    (.on transport
         "connect"
         (fn [parameters callback errback]
           (go
             (if (<! (request! :connectTransport {:type "receive" :parameters parameters}))
               (callback)
               (errback)))))
    (reset! receive-transport transport)))

(defn start-consuming! []
  (go
    (when-not @device (<! (load-device! (<! (request! :capabilities)))))
    (create-receive-transport! (<! (request! :createTransport {:type "receive"})))))

(defonce consumers (atom {}))

(defn consume! [producer-id]
  (go
    (let [parameters (<! (request! :createConsumer {:producerId producer-id}))
          consumer-ch (chan)]
      (-> (.consume @receive-transport parameters)
          (.then #(put! consumer-ch %)))
      (let [consumer (<! consumer-ch)]
        (swap! consumers assoc (.-id consumer) consumer)
        consumer))))
