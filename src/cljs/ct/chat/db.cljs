(ns ct.chat.db
  (:require
   [cljs.spec.alpha :as s]
   [ct.chat.chats.db :as chats.db]
   [ct.chat.connection.db :as connection.db]
   [ct.chat.rooms.db :as rooms.db]
   [ct.chat.media.db :as media.db]))

(s/def ::db (s/merge ::chats.db/chats-keys
                     ::connection.db/connection-keys
                     ::rooms.db/rooms-keys
                     ::media.db/media-keys))

(defn initial-db [config] config)
