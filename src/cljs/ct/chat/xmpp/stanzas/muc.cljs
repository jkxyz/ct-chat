(ns ct.chat.xmpp.stanzas.muc
  "Utilities for Multi-User Chat.

  Specification:
  https://xmpp.org/extensions/xep-0045.html"
  (:require
   [clojure.string :as string]
   [clojure.data.xml :as xml]
   [ct.chat.xmpp.jids :refer [jidparts bare-jid]]
   [ct.chat.xmpp.xml :refer [xml tag= attr= children]]
   [ct.chat.xmpp.namespaces
    :refer [default-ns
            disco-items-ns
            disco-info-ns]]))

(def muc-ns "http://jabber.org/protocol/muc")

(def muc-user-ns "http://jabber.org/protocol/muc#user")

(def muc-admin-ns "http://jabber.org/protocol/muc#admin")

(defn muc-rooms-query-content []
  (xml/element (xml/qname disco-items-ns :query)))

(defn muc-room-presence-stanza [{:keys [from to]} & children]
  (apply
   xml/element
   (xml/qname default-ns :presence)
   {:from from :to to}
   (xml/element (xml/qname muc-ns :x))
   children))

(defn muc-presence? [stanza]
  (and (= (xml/qname default-ns :presence) (:tag stanza))
       (not-empty (sequence (tag= (xml/qname muc-user-ns :x)) [stanza]))))

(defn muc-unavailable-presence? [stanza]
  (and (muc-presence? stanza)
       (= "unavailable" (get-in stanza [:attrs :type]))))

(defn muc-self-presence? [stanza]
  (boolean (and (muc-presence? stanza)
                (not-empty
                 (sequence
                  (comp
                   (tag= (xml/qname muc-user-ns :x))
                   (tag= (xml/qname muc-user-ns :status))
                   (attr= :code "110"))
                  [stanza])))))

(defn muc-kicked-presence? [stanza]
  (boolean (not-empty
            (sequence
             (comp
              (tag= (xml/qname muc-user-ns :x))
              (tag= (xml/qname muc-user-ns :status))
              (attr= :code "307"))
             [stanza]))))

(defn muc-message-stanza [{:keys [from to body]}]
  (xml/element
   (xml/qname default-ns :message)
   {:to to :from from :type "groupchat" :id (str (random-uuid))}
   (xml/element
    (xml/qname default-ns :body)
    {}
    body)))

(defn muc-message? [stanza]
  (not-empty (sequence (comp (attr= :type "groupchat")
                             (tag= (xml/qname default-ns :body)))
                       [stanza])))

(defn muc-presence->occupant [presence-stanza]
  (let [occupant-jid (get-in presence-stanza [:attrs :from])
        {nickname :resource} (jidparts occupant-jid)
        room-jid (bare-jid occupant-jid)
        presence (or (keyword (get-in presence-stanza [:attrs :type]))
                     :available)
        {:keys [jid role affiliation]}
        (:attrs (first (sequence (comp (tag= (xml/qname muc-user-ns :x))
                                       (tag= (xml/qname muc-user-ns :item)))
                                 [presence-stanza])))]
    (cond-> #:occupant {:role (keyword role)
                        :affiliation (keyword affiliation)
                        :occupant-jid occupant-jid
                        :nickname nickname
                        :presence presence
                        :room-jid room-jid
                        :self? (muc-self-presence? presence-stanza)
                        :kicked? (muc-kicked-presence? presence-stanza)}
            jid (assoc :occupant/bare-jid (bare-jid jid)
                       :occupant/username (:local (jidparts jid))))))

(defn muc-kicked-presence [presence-stanza]
  (when (muc-kicked-presence? presence-stanza)
    {:actor-nickname
     (get-in
      (first
       (sequence (comp (tag= (xml/qname muc-user-ns :x))
                       (tag= (xml/qname muc-user-ns :item))
                       (tag= (xml/qname muc-user-ns :actor)))
                 [presence-stanza]))
      [:attrs :nick])}))

(defn iq-result->room-info [iq-result-stanza]
  (let [[identity] (sequence (comp (tag= (xml/qname disco-info-ns :query))
                                   (tag= (xml/qname disco-info-ns :identity)))
                             [iq-result-stanza])]
    {:room/jid (get-in iq-result-stanza [:attrs :from])
     :room/name (get-in identity [:attrs :name])}))

(defn set-muc-role-iq-content [{:keys [nick role]}]
  (xml [(xml/qname muc-admin-ns :query) [:item {:nick nick :role (name role)}]]))

(defn set-muc-affiliation-iq-content [{:keys [affiliation jid]}]
  (xml [(xml/qname muc-admin-ns :query)
        [:item {:affiliation (name affiliation) :jid jid}]]))
