(ns ct.chat.xmpp.stanzas.iq
  (:require [clojure.data.xml :as xml]
            [ct.chat.xmpp.namespaces :refer [default-ns]]))

(defn iq-stanza [attrs content]
  (xml/element (xml/qname default-ns :iq) (update attrs :type name) content))

(defn iq-result? [id stanza]
  (and (= (xml/qname default-ns :iq) (:tag stanza))
       (= "result" (get-in stanza [:attrs :type]))
       (= id (get-in stanza [:attrs :id]))))

(defn iq-error? [id stanza]
  (and (= (xml/qname default-ns :iq) (:tag stanza))
       (= "error" (get-in stanza [:attrs :type]))
       (= id (get-in stanza [:attrs :id]))))
