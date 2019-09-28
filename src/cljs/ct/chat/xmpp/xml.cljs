(ns ct.chat.xmpp.xml
  (:require
   [clojure.data.xml :as xml]))

(def children (mapcat :content))

(defn tagp [pred] (comp children (filter (comp pred :tag))))

(defn tag= [tag] (tagp (partial = tag)))

(defn attr-accessor [a] (comp a :attrs))

(defn attrp [a pred] (filter (comp pred (attr-accessor a))))

(defn attr= [a v] (attrp a (partial = v)))

(defn path [stanza xforms] (sequence (apply comp xforms) [stanza]))

(defn xml [[tag ?attrs & content]]
  (let [content (remove nil? (if (map? ?attrs) content (concat [?attrs] content)))
        attrs (if (map? ?attrs) ?attrs {})]
    (xml/element* tag attrs (map #(if (vector? %) (xml %) %) content))))
