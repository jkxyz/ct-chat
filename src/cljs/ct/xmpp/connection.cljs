(ns ct.xmpp.connection
  (:require
   [cljs.core.async :refer [chan mult put!]]
   [clojure.data.xml :as xml]
   [re-frame.core :as rf]))

(defonce ^:private connection (atom nil))

(defonce ^:private messages-channel (chan))

(defonce messages-mult (mult messages-channel))

(when ^boolean js/goog.DEBUG
  (let [ch (chan)]
    (cljs.core.async/tap messages-mult ch)
    (cljs.core.async/go-loop []
      (js/window.console.debug "<< Received:" (prn-str (cljs.core.async/<! ch)))
      (recur))))

(defn send! [stanza]
  (when ^boolean js/goog.DEBUG (js/window.console.debug ">> Sending:" (prn-str stanza)))
  (.send @connection (xml/emit-str stanza)))

(defn- close-stanza []
  (xml/element (xml/qname "urn:ietf:params:xml:ns:xmpp-framing" :close)))

(defn close-connection! []
  (when @connection
    (send! (close-stanza))
    (.close @connection)
    (reset! connection nil)))

(defn- handle-websocket-message [event]
  (put! messages-channel (xml/parse-str (.-data event))))

(defn reset-connection! [url {:keys [on-open on-error]}]
  (close-connection!)
  (let [c (js/window.WebSocket. url "xmpp")]
    (.addEventListener c "message" handle-websocket-message)
    (.addEventListener c "open" on-open)
    (.addEventListener c "error" on-error)
    (.addEventListener js/window "beforeunload" #(close-connection!))
    (reset! connection c)))
