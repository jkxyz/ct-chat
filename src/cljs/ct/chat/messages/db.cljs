(ns ct.chat.messages.db
  (:require
   [cljs.spec.alpha :as s]
   [ct.chat.xmpp.jids :as jids]))

(s/def :message/type #{:message :status})

(s/def :message/id string?)

(s/def :message/message-type #{:chat :groupchat})

(s/def :message/from-occupant-jid ::jids/full-jid)

(s/def :message/chat-jid ::jids/bare-jid)

(s/def :message/body string?)

(s/def :message/from-jid ::jids/full-jid)

(s/def :message/from-username ::jids/local)

(s/def :message/from-nickname ::jids/resource)

(defmulti message-spec :message/type)

(defmethod message-spec :message [_]
  (s/keys :req [:message/type
                :message/id
                :message/message-type
                :message/from-occupant-jid
                :message/chat-jid
                :message/body
                :message/from-username
                :message/from-nickname]
          :opt [:message/from-jid]))

(s/def :message/action #{:kicked})

(s/def :message/actor-occupant-jid ::jids/full-jid)

(s/def :message/occupant-jid ::jids/full-jid)

(defmethod message-spec :status [_]
  (s/keys :req [:message/type
                :message/id
                :message/action
                :message/actor-occupant-jid
                :message/occupant-jid]))

(s/def ::message (s/multi-spec message-spec :message/type))

(s/def :messages/messages (s/map-of ::jids/bare-jid (s/coll-of ::message)))

(s/def ::messages-keys (s/keys :req [:messages/messages]))
