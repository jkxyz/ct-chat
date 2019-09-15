(ns ct.chat.rooms.db
  (:require
   [cljs.spec.alpha :as s]
   [ct.chat.jids :as jids]))

(s/def :rooms/default-nickname ::jids/resource)

(s/def :rooms/default-room-jid ::jids/bare-jid)

(s/def :occupant/role #{:participant})

(s/def :occupant/presence #{:available})

(s/def :occupant/affiliation #{:none})

(s/def :occupant/nickname ::jids/resource)

(s/def :occupant/username ::jids/local)

(s/def :occupant/self? boolean?)

(s/def :occupant/occupant-jid ::jids/full-jid)

(s/def :occupant/bare-jid ::jids/bare-jid)

(s/def :occupant/room-jid ::jids/bare-jid)

(s/def ::occupant
  (s/keys :req [:occupant/role
                :occupant/presence
                :occupant/affiliation
                :occupant/nickname
                :occupant/username
                :occupant/self?
                :occupant/occupant-jid
                :occupant/bare-jid
                :occupant/room-jid]))

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
