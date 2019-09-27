(ns ct.chat.xmpp.stanzas.message
  (:require
   [clojure.string :as string]
   [clojure.data.xml :as xml]
   [ct.chat.xmpp.xml :refer [tag= attr= children]]
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

(def address-ns "http://jabber.org/protocol/address")

(defn message-addresses [message-stanza]
  (let [addresses (sequence (tag= (xml/qname address-ns :addresses)) [message-stanza])]
    (if-let [content (:content (first addresses))]
      (into {} (map (juxt (comp keyword :type :attrs) (comp :jid :attrs)) content))
      nil)))

(defn message [message-stanza]
  (let [from (get-in message-stanza [:attrs :from])
        type (or (keyword (get-in message-stanza [:attrs :type])) :chat)
        chat-jid (if (= :groupchat type) (first (string/split from "/")) from)
        addresses (message-addresses message-stanza)]
    (cond-> {:message/type :message
             :message/id (get-in message-stanza [:attrs :id])
             :message/message-type type
             :message/from-occupant-jid from
             :message/chat-jid chat-jid
             :message/body (message-body message-stanza)}
      addresses (assoc :message/from-jid (:ofrom addresses)))))
