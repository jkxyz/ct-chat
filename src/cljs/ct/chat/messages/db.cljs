(ns ct.chat.messages.db
  (:require
   [cljs.spec.alpha :as s]
   [ct.chat.xmpp.jids :as jids]))

(s/def ::message (s/keys :req []))

(s/def :messages/messages (s/map-of ::jids/bare-jid (s/coll-of ::message)))

(s/def ::messages-keys (s/keys :req [:messages/messages]))
