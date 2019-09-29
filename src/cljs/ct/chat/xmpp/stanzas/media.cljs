(ns ct.chat.xmpp.stanzas.media
  (:require
   [clojure.data.xml :as xml]
   [ct.chat.xmpp.xml :refer [select tag=]]))

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

(defn presence->producer [presence-stanza]
  (when-let [producer (select presence-stanza (tag= (xml/qname ::media :producer)))]
    {:producer/id (get-in producer [:attrs :id])}))
