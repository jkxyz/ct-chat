(ns ct.chat.media.db
  (:require
   [cljs.spec.alpha :as s]))

(s/def :media/signalling-websocket-uri string?)

(s/def ::media-keys
  (s/keys :req [:media/signalling-websocket-uri]))
