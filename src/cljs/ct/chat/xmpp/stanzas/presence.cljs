(ns ct.chat.xmpp.stanzas.presence
  (:require [clojure.data.xml :as xml]
            [ct.chat.xmpp.namespaces :refer [default-ns]]))

(defn presence-stanza [{:keys [from to]}]
  (xml/element
   (xml/qname default-ns :presence)
   (cond-> {}
     from (assoc :from from)
     to (assoc :to to))))
