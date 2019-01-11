(ns ct.chat.media.signalling.peer
  (:require
   [cljs.core.async :refer [chan put!]]
   [protoo-client :as protoo]))

(defonce ^:private transport (atom nil))

(defonce ^:private peer (atom nil))

(defonce ^:private notification-handlers (atom {}))

(defn- handle-notification [message]
  (let [method (keyword (.-method message))]
    ((@notification-handlers method) message)))

(defn set-notification-handler! [method handler-fn]
  (swap! notification-handlers assoc method handler-fn))

(defn initialize! [{:keys [url]}]
  (reset! transport (protoo/WebSocketTransport. url))
  (reset! peer (protoo/Peer. @transport))
  (.on @peer "notification" handle-notification))

(defn request!
  ([method] (request! method nil))
  ([method data]
   (let [ch (chan)]
     (-> (.request @peer (name method) (clj->js data))
         (.then #(put! ch %))
         (.catch #(put! ch ::rejected)))
     ch)))
