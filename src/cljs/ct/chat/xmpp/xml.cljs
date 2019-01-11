(ns ct.chat.xmpp.xml)

(def children (mapcat :content))

(defn tagp [pred] (comp children (filter (comp pred :tag))))

(defn tag= [tag] (tagp (partial = tag)))

(defn attr-accessor [a] (comp a :attrs))

(defn attrp [a pred] (filter (comp pred (attr-accessor a))))

(defn attr= [a v] (attrp a (partial = v)))

(defn path [stanza xforms] (sequence (apply comp xforms) [stanza]))
