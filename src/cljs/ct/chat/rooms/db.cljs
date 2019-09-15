(ns ct.chat.rooms.db
  (:require
   [cljs.spec.alpha :as s]
   [ct.chat.jids :as jids]))

(s/def :rooms/default-nickname string?)

(s/def :rooms/default-room-jid ::jids/bare-jid)

(s/def ::role #{:participant})

(s/def ::presence #{:available})

(s/def ::affiliation #{:none})

(s/def ::nickname string?)

(s/def ::username string?)

(s/def ::self? boolean?)

(s/def ::occupant-jid ::jids/full-jid)

(s/def ::bare-jid ::jids/bare-jid)

(s/def ::room-jid ::jids/bare-jid)

(s/def ::occupant
  (s/keys :req-un [::role
                   ::presence
                   ::affiliation
                   ::nickname
                   ::username
                   ::self?
                   ::occupant-jid
                   ::bare-jid
                   ::room-jid]))

(s/def ::occupants (s/map-of ::jids/full-jid ::occupant))

(s/def :rooms/occupants (s/map-of ::jids/bare-jid ::occupants))

(s/def ::jid ::jids/bare-jid)

(s/def ::name string?)

(s/def ::room (s/keys :req-un [::jid ::name]))

(s/def :rooms/rooms (s/map-of ::jids/bare-jid ::room))

(s/def ::rooms-keys
  (s/keys :req [:rooms/default-nickname
                :rooms/default-room-jid
                :rooms/occupants]))
