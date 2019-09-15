(ns ct.chat.connection.db
  (:require
   [cljs.spec.alpha :as s]
   [ct.chat.jids :as jids]))

(s/def ::re-frame-event (s/cat :event keyword? :args (s/* any?)))

(s/def :connection/password string?)

(s/def :connection/server-bare-jid ::jids/domain)

(s/def :connection/on-ready ::re-frame-event)

(s/def :connection/bare-jid ::jids/bare-jid)

(s/def :connection/websocket-uri string?)

(s/def :connection/full-jid ::jids/full-jid)

(s/def ::connection-keys
  (s/keys :req [:connection/password
                :connection/server-bare-jid
                :connection/on-ready
                :connection/bare-jid
                :connection/websocket-uri
                :connection/full-jid]))
