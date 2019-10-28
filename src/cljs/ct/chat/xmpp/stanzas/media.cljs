(ns ct.chat.xmpp.stanzas.media
  (:require
   [clojure.data.xml :as xml]
   [ct.chat.xmpp.xml :refer [xml select tag=]]
   [ct.chat.xmpp.namespaces :refer [default-ns]]))

;; (defn media-producer-element? [element]
;;   (= (xml/qname ::media :producer) (:tag element)))

;; (defn has-child-media-producer-element? [stanza]
;;   (some media-producer-element? (:content stanza)))

;; (defn child-media-producer-element [stanza]
;;   (first (filter media-producer-element? (:content stanza))))

;; (defn producer-presence-stanza [{:keys [from to producer-id]}]
;;   (xml/element
;;    (xml/qname default-ns :presence)
;;    {:from from :to to}
;;    (xml/element (xml/qname ::media :producer) {:id producer-id})))

(defn presence->producers [presence-stanza]
  (seq
   (for [p (select presence-stanza (tag= (xml/qname :ct.chat.media :producer)))]
     {:producer/id (get-in p [:attrs :id])
      :producer/type (keyword (get-in p [:attrs :type]))
      :producer/occupant-jid (get-in presence-stanza [:attrs :from])})))

(defn media-presence-stanza [{:keys [from to video-producer-id audio-producer-id]}]
  (xml [(xml/qname default-ns :presence)
        {:from from :to to}
        [(xml/qname :ct.chat.media :producer) {:type "video" :id video-producer-id}]
        [(xml/qname :ct.chat.media :producer) {:type "audio" :id audio-producer-id}]]))
