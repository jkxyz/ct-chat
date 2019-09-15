(ns ct.chat.chats.db
  (:require
   [cljs.spec.alpha :as s]
   [ct.chat.jids :as jids]))

(s/def :chat/jid ::jids/bare-jid)

(s/def :chat/type #{:chat :groupchat})

(s/def :chat/from-jid ::jids/full-jid)

(s/def :chat/unread-messages-count integer?)

(s/def ::chat
  (s/keys :req [:chat/jid
                :chat/type
                :chat/from-jid]
          :opt [:chat/unread-messages-count]))

(s/def :chats/chats (s/map-of ::jids/bare-jid ::chat))

(s/def :chats/active-chat-jid ::jids/bare-jid)

(s/def ::chats-keys
  (s/keys :req [:chats/active-chat-jid
                :chats/chats]))
