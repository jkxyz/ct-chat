(ns ct.chat.media.device
  (:require
   [cljs.core.async :refer [chan close! go <! put!]]
   [ajax.core :as ajax]
   [mediasoup-client :as mediasoup]))

(defonce ^:private user-media-stream (atom nil))

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

(defn- get-capabilities []
  (let [ch (chan)]
    (ajax/ajax-request
     {:uri "http://localhost:3500/capabilities"
      :method :get
      :response-format (ajax/raw-response-format)
      :handler
      (fn [[ok response]]
        (if ok
          (put! ch (js/window.JSON.parse response))
          (close! ch)))})
    ch))

(defn- load-device! [capabilities]
  (let [ch (chan)]
    (go
      (let [new-device (mediasoup/Device.)]
        (-> (.load new-device (clj->js {:routerRtpCapabilities capabilities}))
            (.then (fn [] (close! ch))))
        (reset! device new-device)))
    ch))

(defn- create-server-transport! []
  (let [ch (chan)]
    (ajax/ajax-request
     {:uri "http://localhost:3500/transports"
      :method :post
      :format (ajax/text-request-format)
      :response-format (ajax/raw-response-format)
      :handler
      (fn [[ok response]]
        (if ok
          (put! ch (js/window.JSON.parse response))
          (close! ch)))})
    ch))

(defn- connect-server-transport! [transport-id parameters]
  (js/console.debug "Connecting to server transport" transport-id parameters)
  (let [ch (chan)]
    (ajax/ajax-request
     {:uri (str "http://localhost:3500/transports/" transport-id "/connect")
      :method :post
      :format (ajax/json-request-format)
      :response-format (ajax/raw-response-format)
      :params parameters
      :handler
      (fn [[ok response]]
        (if ok
          (put! ch :ok)
          (close! ch)))})
    ch))

(defn create-server-producer! [transport-id parameters]
  (let [ch (chan)]
    (ajax/ajax-request
     {:uri (str "http://localhost:3500/transports/" transport-id "/producers")
      :method :post
      :format (ajax/json-request-format)
      :response-format (ajax/raw-response-format)
      :params parameters
      :handler
      (fn [[ok response]]
        (if ok
          (put! ch (js/window.JSON.parse response))
          (close! ch)))})
    ch))

(defonce ^:private send-transport (atom nil))

(defn- create-send-transport! [parameters]
  (let [transport (.createSendTransport @device (clj->js parameters))]
    (doto transport
      (.on "connect"
           (fn [parameters callback errback]
             (go
               (if (<! (connect-server-transport! (.-id transport) parameters))
                 (callback)
                 (errback)))))
      (.on "produce"
           (fn [parameters callback errback]
             (go
               (if-let [p (<! (create-server-producer! (.-id transport) parameters))]
                 (callback p)
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

(defn start-broadcasting! []
  (go
    (<! (load-device! (<! (get-capabilities))))
    (create-send-transport! (<! (create-server-transport!)))
    (let [video-producer (<! (produce-video!))
          audio-producer (<! (produce-audio!))]
      {:video-producer-id (.-id video-producer)
       :audio-producer-id (.-id audio-producer)})))
