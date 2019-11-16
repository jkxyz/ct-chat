(ns ct.chat
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [ct.chat.events :as events]
   [ct.chat.views :as views])
  (:import goog.Uri))

(defn mount-root []
  (reagent/render [views/app] (js/document.getElementById "app")))

(defn- jid->nickname [jid] (first (string/split jid "@")))

(defn ^:export initialize [options]
  (let [{:keys [jid password roomJid serverJid websocketUri mediaWebSocketUri]}
        (js->clj options :keywordize-keys true)]
    (rf/dispatch-sync
     [::events/initialize
      {:connection/bare-jid jid
       :connection/password password
       :connection/server-bare-jid serverJid
       :connection/websocket-uri websocketUri
       :rooms/default-room-jid roomJid
       :rooms/default-nickname (jid->nickname jid)
       :media/signalling-websocket-uri mediaWebSocketUri}])
    (mount-root)))
