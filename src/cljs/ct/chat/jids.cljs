(ns ct.chat.jids
  "The specification for the JID format is defined in RFC 6122:
  https://tools.ietf.org/html/rfc6122"
  (:require
   [cljs.spec.alpha :as s]
   [clojure.string :as string]))

(s/def ::non-blank-string (s/and string? (complement string/blank?)))

(s/def ::domain ::non-blank-string)
(s/def ::local ::non-blank-string)
(s/def ::resource ::non-blank-string)

(s/def ::jidparts (s/keys :req-un [::domain] :opt-un [::local ::resource]))

(defn jid [{:keys [domain local resource] :as jidparts}]
  {:pre [(s/valid? ::jidparts jidparts)]}
  (cond-> (if local (str local "@" domain) domain)
    resource (str "/" resource)))

;; N.B. This will match invalid JIDs like "local@domain/"
(def ^:private jid-regex #"(([^@]+)@)?([^/]+)(/(.+))?")

(defn jidparts [jid]
  (let [[_ _ local domain _ resource] (re-matches jid-regex jid)]
    (cond-> {:domain domain}
      local (assoc :local local)
      resource (assoc :resource resource))))

(s/def ::jid
  (s/and string? (s/conformer #(s/conform ::jidparts (jidparts %)) jid)))

(s/def ::bare-jid (s/and ::jid (complement :resource)))

(s/def ::full-jid (s/and ::jid :domain :local :resource))

(defn bare-jid [jid]
  (let [{:keys [local domain]} (jidparts jid)]
    (str local "@" domain)))
