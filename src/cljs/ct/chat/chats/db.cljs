(ns ct.chat.chats.db
  (:require
   [cljs.spec.alpha :as s]
   [ct.chat.jids :as jids]))

(s/def ::jid ::jids/bare-jid)

(s/def ::type #{:groupchat})

(s/def ::from-jid ::jids/full-jid)

(s/def ::chat
  (s/keys :req-un [::jid
                   ::type
                   ::from-jid]))

(s/def :chats/chats (s/map-of ::jids/bare-jid ::chat))

(s/def :chats/active-chat-jid ::jids/bare-jid)

(s/def ::chats-keys
  (s/keys :req [:chats/active-chat-jid
                :chats/chats]))
