(ns ct.chat.xmpp.stanzas.sasl
  (:require [clojure.string :as string]
            [clojure.data.xml :as xml]
            [ct.chat.xmpp.namespaces :refer [streams-ns sasl-ns]]))

(defn- filter-by-tag [tag els] (filter #(= tag (:tag %)) els))

(defn features-supports-sasl-plain? [stanza]
  {:pre [(= (xml/qname streams-ns :features) (:tag stanza))]}
  (when-let [ms (first (filter-by-tag (xml/qname sasl-ns :mechanisms)
                                      (:content stanza)))]
    (some #(= "PLAIN" (first (:content %))) (:content ms))))

(defn- sasl-plain-message
  ([authcid passwd] (sasl-plain-message nil authcid passwd))
  ([authzid authcid passwd]
   (string/join "\u0000" [(or authzid nil) authcid passwd])))

(defn sasl-plain-auth-stanza [jid password]
  (xml/element
   (xml/qname sasl-ns :auth)
   {:mechanism "PLAIN"}
   (js/btoa (sasl-plain-message jid password))))

(defn sasl-auth-success? [stanza]
  (= (xml/qname sasl-ns :success) (:tag stanza)))
