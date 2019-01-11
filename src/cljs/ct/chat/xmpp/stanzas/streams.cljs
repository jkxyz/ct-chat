(ns ct.chat.xmpp.stanzas.streams
  (:require [clojure.data.xml :as xml]
            [ct.chat.xmpp.namespaces :refer [streams-ns]]))

(defn features-stanza? [stanza]
  (= (xml/qname streams-ns :features) (:tag stanza)))
