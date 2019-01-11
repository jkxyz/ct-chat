(ns ct.chat.xmpp.stanzas.message
  (:require
   [clojure.string :as string]
   [clojure.data.xml :as xml]
   [ct.chat.xmpp.xml :refer [tag= children]]
   [ct.chat.xmpp.namespaces :refer [default-ns]]))

(defn message-stanza? [stanza]
  (and (= (xml/qname default-ns :message) (:tag stanza))
       (not-empty (sequence (tag= (xml/qname default-ns :body)) [stanza]))))

(defn message-stanza [{:keys [from to body]}]
  (xml/element
   (xml/qname default-ns :message)
   {:to to :from from :id (str (random-uuid)) :type "chat"}
   (xml/element (xml/qname default-ns :body) {} body)))

(defn message-body [message-stanza]
  (apply str (sequence (comp (tag= (xml/qname default-ns :body))
                             children)
                       [message-stanza])))

(defn message [message-stanza]
  (let [from (get-in message-stanza [:attrs :from])
        type (or (keyword (get-in message-stanza [:attrs :type])) :chat)]
    {:chat-jid (condp = type :chat from :groupchat (first (string/split from "/")))
     :body (message-body message-stanza)
     :from from
     :id (get-in message-stanza [:attrs :id])
     :type type}))
