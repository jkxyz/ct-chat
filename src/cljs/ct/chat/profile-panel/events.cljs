(ns ct.chat.profile-panel.events
  (:require
   [re-frame.core :as rf]
   [ct.chat.xmpp.stanzas.muc
    :refer [set-muc-role-iq-content
            set-muc-affiliation-iq-content
            muc-unavailable-presence?]]))

(rf/reg-event-fx
 ::initialize
 (fn [_ _]
   {:xmpp/add-listener
    {:id ::unavailable-presence-listener
     :xform (filter muc-unavailable-presence?)
     :on-message [::unavailable-room-presence-received]}}))

(rf/reg-event-fx
 ::unavailable-room-presence-received
 (fn [{:keys [db]} [_ presence-stanza]]
   (let [occupant-jid (get-in presence-stanza [:attrs :from])]
     (if (= (:profile-panel/occupant-jid db) occupant-jid)
       {:db (assoc db :profile-panel/open? false)}
       {}))))

(rf/reg-event-fx
 ::close-button-clicked
 (fn [{:keys [db]}]
   {:db (assoc db :profile-panel/open? false)}))

(rf/reg-event-fx
 ::action-selected
 (fn [{:keys [db]} [_ type]]
   (let [{:profile-panel/keys [chat-jid occupant-jid]} db
         target-occupant (get-in (:rooms/occupants db) [chat-jid occupant-jid])]
     {:dispatch
      [(case type
         :kick ::kick-occupant-selected
         :revoke-voice ::revoke-voice-selected
         :grant-voice ::grant-voice-selected
         :grant-moderator-status ::grant-moderator-status-selected
         :revoke-moderator-status ::revoke-moderator-status-selected
         :ban ::ban-selected)
       {:target-occupant target-occupant}]})))

(rf/reg-event-fx
 ::kick-selected
 (fn [{:keys [db]} [_ {:keys [target-occupant]}]]
   {:xmpp/query
    {:type :set
     :content (set-muc-role-iq-content
               {:nick (:occupant/nickname target-occupant)
                :role :none})
     :from (:connection/full-jid db)
     :to (:occupant/room-jid target-occupant)
     :on-result [::kick-occupant-result-received]}}))

(rf/reg-event-fx
 ::kick-occupant-result-received
 (fn [_ _]
   {}))

(rf/reg-event-fx
 ::revoke-voice-selected
 (fn [{:keys [db]} [_ {:keys [target-occupant]}]]
   {:xmpp/query
    {:type :set
     :content (set-muc-role-iq-content
               {:nick (:occupant/nickname target-occupant)
                :role :visitor})
     :from (:connection/full-jid db)
     :to (:occupant/room-jid target-occupant)
     :on-result [::revoke-voice-result-received]}}))

(rf/reg-event-fx
 ::revoke-voice-result-received
 (fn [_ _]
   {}))

(rf/reg-event-fx
 ::grant-voice-selected
 (fn [{:keys [db]} [_ {:keys [target-occupant]}]]
   {:xmpp/query
    {:type :set
     :content (set-muc-role-iq-content
               {:nick (:occupant/nickname target-occupant)
                :role :participant})
     :from (:connection/full-jid db)
     :to (:occupant/room-jid target-occupant)
     :on-result [::grant-voice-result-received]}}))

(rf/reg-event-fx
 ::grant-voice-result-received
 (fn [_ _]
   {}))

(rf/reg-event-fx
 ::grant-moderator-status-selected
 (fn [{:keys [db]} [_ {:keys [target-occupant]}]]
   {:xmpp/query
    {:type :set
     :content (set-muc-role-iq-content
               {:nick (:occupant/nickname target-occupant)
                :role :moderator})
     :from (:connection/full-jid db)
     :to (:occupant/room-jid target-occupant)
     :on-result [::grant-moderator-status-result-received]}}))

(rf/reg-event-fx
 ::grant-moderator-status-result-received
 (fn [_ _]
   {}))

(rf/reg-event-fx
 ::revoke-moderator-status-selected
 (fn [{:keys [db]} [_ {:keys [target-occupant]}]]
   {:xmpp/query
    {:type :set
     :content (set-muc-role-iq-content
               {:nick (:occupant/nickname target-occupant)
                :role :participant})
     :from (:connection/full-jid db)
     :to (:occupant/room-jid target-occupant)
     :on-result [::revoke-moderator-status-result-received]}}))

(rf/reg-event-fx
 ::revoke-moderator-status-result-received
 (fn [_ _]
   {}))

(rf/reg-event-fx
 ::ban-selected
 (fn [{:keys [db]} [_ {:keys [target-occupant]}]]
   {:xmpp/query
    {:type :set
     :content (set-muc-affiliation-iq-content
               {:jid (:occupant/bare-jid target-occupant)
                :affiliation :outcast})
     :from (:connection/full-jid db)
     :to (:occupant/room-jid target-occupant)
     :on-result [::ban-result-received]}}))

(rf/reg-event-fx
 ::ban-result-received
 (fn [_ _]
   {}))
