(ns ct.chat.xmpp.stanzas.binding
  (:require [clojure.data.xml :as xml]
            [ct.chat.xmpp.namespaces :refer [binding-ns default-ns]]))

(defn features-supports-bind? [features-stanza]
  (some #(= (xml/qname binding-ns :bind) (:tag %)) (:content features-stanza)))

(defn bind-query-content []
  (xml/element (xml/qname binding-ns :bind)))

;; TODO: Use querySelector??
(defn bind-jid [result-stanza]
  (first (:content (first (:content (first (:content result-stanza)))))))
