(ns ct.chat.profile-panel.subscriptions
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 ::occupant
 (fn [{:profile-panel/keys [open? chat-jid occupant-jid] :rooms/keys [occupants]}]
   (when open? (get-in occupants [chat-jid occupant-jid]))))

(rf/reg-sub
 ::self-occupant
 (fn [{:profile-panel/keys [chat-jid] :rooms/keys [occupants]}]
   (val (first (filter (comp :occupant/self? val) (get occupants chat-jid))))))

(defn- can-kick? [self-occupant target-occupant]
  (and (= :moderator (:occupant/role self-occupant))
       (#{:visitor :participant} (:occupant/role target-occupant))))

(defn- can-grant-voice? [self-occupant target-occupant]
  (and (= :moderator (:occupant/role self-occupant))
       (= :visitor (:occupant/role target-occupant))))

(defn- can-revoke-voice? [self-occupant target-occupant]
  (and (= :moderator (:occupant/role self-occupant))
       (= :participant (:occupant/role target-occupant))))

(defn- can-ban? [self-occupant target-occupant]
  (and (#{:admin :owner} (:occupant/affiliation self-occupant))
       (#{:none :member} (:occupant/affiliation target-occupant))))

(defn- can-assign-moderator-role? [self-occupant target-occupant]
  (and (#{:admin :owner} (:occupant/affiliation self-occupant))
       (not= :moderator (:occupant/role target-occupant))))

(defn- can-revoke-moderator-role? [self-occupant target-occupant]
  (and (#{:admin :owner} (:occupant/affiliation self-occupant))
       (#{:none :member} (:occupant/affiliation target-occupant))
       (= :moderator (:occupant/role target-occupant))))

(rf/reg-sub
 ::available-actions
 :<- [::self-occupant]
 :<- [::occupant]
 (fn [[self target]]
   (when target
     (cond-> []
       (can-kick? self target) (conj :kick)
       (can-grant-voice? self target) (conj :grant-voice)
       (can-revoke-voice? self target) (conj :revoke-voice)
       (can-ban? self target) (conj :ban)
       (can-assign-moderator-role? self target) (conj :grant-moderator-status)
       (can-revoke-moderator-role? self target) (conj :revoke-moderator-status)
       :always (not-empty)))))
