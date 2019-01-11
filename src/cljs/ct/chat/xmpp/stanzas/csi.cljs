(ns ct.chat.xmpp.stanzas.csi
  "Utilities for the Client State Indication extension.

  Specification:
  https://xmpp.org/extensions/xep-0352.html"
  (:require [clojure.data.xml :as xml]
            [ct.chat.xmpp.namespaces :refer [csi-ns]]))

(defn active-stanza []
  (xml/element (xml/qname csi-ns :active)))

(defn inactive-stanza []
  (xml/element (xml/qname csi-ns :inactive)))
