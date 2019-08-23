(ns ct.chat.core
  (:require [re-frame.core :as rf]
            [reagent.core :as reagent]
            [ct.xmpp.effects]
            [ct.chat.events :as events]
            [ct.chat.views :as views]
            [mediasoup-client :as mediasoup]
            [protoo-client :as protoo])
  (:import goog.Uri))

(defn mount-root []
  (reagent/render [views/app] (.getElementById js/document "app")))

(defonce ^:private bare-jid js/window.CT_CHAT_JID)

(defonce ^:private password js/window.CT_CHAT_PASSWORD)

(def default-room-jid "test-room@chat")

(def default-nickname (first (clojure.string/split bare-jid "@")))

(defn init! []
  (mount-root)
  (rf/dispatch-sync
   [::events/initialize
    {:connection/bare-jid bare-jid
     :connection/password password
     :connection/server-bare-jid "localhost"
     :connection/websocket-uri (.setDomain (Uri. "ws://localhost:5443/ws/") js/window.location.hostname)
     :rooms/default-room-jid default-room-jid
     :rooms/default-nickname default-nickname
     :media/signalling-websocket-uri (.setDomain (Uri. "ws://localhost:3500/") js/window.location.hostname)}]))

(comment
  (do
    (def transport (protoo/WebSocketTransport. "ws://localhost:3500"))
    (def peer (protoo/Peer. transport))

    (def device (mediasoup/Device.))

    (def send-transport (atom nil))
    (def recv-transport (atom nil))

    (.on peer "notification"
         (fn [m]
           (js/console.log m)
           (condp = (.-method m)
             "capabilities"
             (.then (.load device #js {:routerRtpCapabilities (.-data m)})
                    (fn [] (.notify peer "loaded")))
             "send-transport"
             (let [transport (.createSendTransport device (.-data m))]
               (.on transport "connect"
                    (fn [data callback errback]
                      (.then (.notify peer "send-transport-connect" data) #(callback))))
               (.on transport "produce"
                    (fn [parameters callback errback]
                      (.then (.request peer "send-transport-produce" parameters)
                             (fn [data] (callback data)))))
               (.then
                (js/navigator.mediaDevices.getUserMedia #js {:video true})
                (fn [stream]
                  (let [track (get (.getVideoTracks stream) 0)]
                    (.then
                     (.produce
                      transport
                      (clj->js {:track track :encodings [{:maxBitrate 100}] :codecOptions {:videoGoogleStartBitrate 1000}}))
                     (fn [producer]
                       (js/console.log "Local producer: " (.-id producer)))))))
               (reset! send-transport transport))
             "recv-transport"
             (let [transport (.createRecvTransport device (.-data m))]
               (.on transport "connect"
                    (fn [data callback errback]
                      (.then (.notify peer "recv-transport-connect" data) #(callback))))
               (reset! recv-transport transport))
             "consumer"
             (.then
              (.consume @recv-transport (.-data m))
              (fn [consumer]
                (let [v (js/document.createElement "video")]
                  (set! (.-srcObject v) (js/MediaStream. #js [(.-track consumer)]))
                  (js/document.body.appendChild v)
                  (set! (.-volume v) 0)
                  (.setAttribute v "producer" (.-producerId consumer))
                  (.play v)))))))
    )


  )
