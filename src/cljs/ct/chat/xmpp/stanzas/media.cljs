(ns ct.chat.xmpp.stanzas.media
  (:require
   [clojure.data.xml :as xml]
   [ct.chat.xmpp.xml :refer [xml select tag=]]
   [ct.chat.xmpp.namespaces :refer [default-ns]]))

(defn presence->producers [presence-stanza]
  (seq
   (for [p (select presence-stanza (tag= (xml/qname :ct.chat.media :producer)))]
     {:producer/id (get-in p [:attrs :id])
      :producer/kind (keyword (get-in p [:attrs :kind]))
      :producer/occupant-jid (get-in presence-stanza [:attrs :from])})))

(defn media-presence-stanza [{:keys [from to video-producer-id audio-producer-id]}]
  (xml [(xml/qname default-ns :presence)
        {:from from :to to}
        [(xml/qname :ct.chat.media :producer) {:kind "video" :id video-producer-id}]
        [(xml/qname :ct.chat.media :producer) {:kind "audio" :id audio-producer-id}]]))
