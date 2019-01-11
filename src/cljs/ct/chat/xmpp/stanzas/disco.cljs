(ns ct.chat.xmpp.stanzas.disco
  "Utilities for the Service Discovery extensions.

  Specification:
  https://xmpp.org/extensions/xep-0128.html"
  (:require
   [clojure.data.xml :as xml]
   [ct.chat.xmpp.namespaces
    :refer [disco-items-ns
            disco-info-ns]]))

(defn disco-items-query-content []
  (xml/element (xml/qname disco-items-ns :query)))

(defn disco-info-query-content []
  (xml/element (xml/qname disco-info-ns :query)))
