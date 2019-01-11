(ns ct.chat.xmpp.stanzas.framing
  (:require [clojure.data.xml :as xml]
            [ct.chat.xmpp.namespaces :refer [framing-ns]]))

(defn initial-open-stanza [{:keys [to]}]
  (xml/element (xml/qname framing-ns :open) {:to to :version "1.0"}))

(defn open-stanza [{:keys [from to]}]
  (xml/element (xml/qname framing-ns :open)
               {:from from
                :to to
                :version "1.0"
                (xml/qname :xml :lang) "en"}))

(defn close-stanza []
  (xml/element (xml/qname framing-ns :close)))
