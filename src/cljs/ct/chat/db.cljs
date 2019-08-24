(ns ct.chat.db
  (:require
   [clojure.string :as string]
   [cljs.spec.alpha :as s]))

(s/def ::bare-jid (s/and string? #(= 2 (count (string/split % "@")))))

(s/def ::room-jid ::bare-jid)

(s/def ::occupant-jid (s/and string?
                             (fn [s]
                               (let [[room-jid nick] (string/split s "/")]
                                 (and (string? nick)
                                      (= 2 (count (string/split room-jid "@"))))))))

(s/def :connection/bare-jid ::bare-jid)
(s/def :connection/password string?)
(s/def :connection/server-bare-jid string?)
(s/def :connection/websocket-uri string?)

(s/def ::db (s/keys :req [:connection/bare-jid
                          :connection/password
                          :connection/server-bare-jid
                          :connection/websocket-uri]))

(defn initial-db [config] {:post [(s/valid? ::db %)]} config)
