(ns ct.chat.media.events
  (:require
   [re-frame.core :as rf]
   [day8.re-frame.http-fx]
   [ajax.core :as ajax]
   [ct.chat.xmpp.stanzas.muc :refer [muc-presence?]]
   [ct.chat.xmpp.stanzas.media
    :refer [presence->producers
            media-presence-stanza]]
   [ct.chat.media.effects :as fx]))

(rf/reg-event-fx
 ::initialize
 (fn [{:keys [db]} _]
   {:db (assoc db :media/signalling-connected? false)}))

;; Broadcasting
;; ============

(rf/reg-event-fx
 ::start-broadcast-button-clicked
 (fn [{:keys [db]} _]
   {:db (assoc db :media/broadcast-chat-jid (:chats/active-chat-jid db))
    ::fx/request-user-media
    {:on-ready [::user-media-ready]
     :on-error [::user-media-error-occurred]}}))

(rf/reg-event-fx
 ::user-media-ready
 (fn [{:keys [db]} _]
   (if-not (:media/signalling-connected? db)
     {::fx/connect
      {:url (:media/websocket-uri db)
       :on-open [::connection-ready-for-broadcast]}}
     {:dispatch [::connection-ready-for-broadcast]})))

(rf/reg-event-fx
 ::user-media-error-occurred
 (fn [_ _]
   nil))

(rf/reg-event-fx
 ::connection-ready-for-broadcast
 (fn [{:keys [db]} _]
   {:db (assoc db :media/signalling-connected? true)
    ::fx/start-broadcasting {:on-ready [::broadcasting-ready]}}))

(rf/reg-event-fx
 ::broadcasting-ready
 (fn [{:keys [db]} [_ {:keys [video-producer-id audio-producer-id]}]]
   (let [{:connection/keys [full-jid] :media/keys [broadcast-chat-jid]} db]
     {:db (assoc db
                 :media/broadcasting? true
                 :media/video-producer-id video-producer-id
                 :media/audio-producer-id audio-producer-id)
      :xmpp/send {:stanza (media-presence-stanza
                           {:from full-jid
                            :to broadcast-chat-jid
                            :video-producer-id video-producer-id
                            :audio-producer-id audio-producer-id})}})))

;; (rf/reg-event-fx
;;  ::error-occurred
;;  (fn [_ [_ message]]
;;    (js/window.alert (str "Error: " message))
;;    {}))

;; (rf/reg-event-fx
;;  ::initialize
;;  (fn [{:keys [db]} _]
;;    {:db (assoc db :media/connection-state :closed)
;;     :xmpp/add-listener
;;     {:id ::room-presence-listener
;;      :xform (filter muc-presence?)
;;      :on-message [::room-presence-received]}}))

;; (rf/reg-event-fx
;;  ::room-presence-received
;;  (fn [{:keys [db]} [_ presence-stanza]]
;;    (let [occupant-jid (get-in presence-stanza [:attrs :from])]
;;      (if-let [producers (presence->producers presence-stanza)]
;;        (let [should-connect? (= :closed (:media/connection-state db))]
;;          (cond-> {:db (assoc-in db [:media/producers occupant-jid] producers)}
;;            should-connect?
;;            (-> (assoc-in [:db :media/connection-state] :opening)
;;                (assoc ::fx/connect
;;                       {:url (:media/websocket-uri db)
;;                        :on-open [::connection-opened]
;;                        :on-error [::error-occurred "Media server connection error."]
;;                        :on-close [::connection-closed]}))))
;;        {:db (update db :media/producers dissoc occupant-jid)}))))

;; ;; Don't put the connection code here
;; ;; Initialize the connection with config
;; ;; Open connection on first consume/produce
;; ;; Close connection when user requests to not show media

;; (rf/reg-event-fx
;;  :connection-closed
;;  (fn [{:keys [db]} _]
;;    {:db (assoc db :media/connection-state :closed)}))

;; (rf/reg-event-fx
;;  ::connection-opened
;;  (fn [{:keys [db]} _]
;;    {:db (assoc db :media/connection-state :open)
;;     ::fx/send {:message {:type "initialize"}}}))

;; ;; BROADCASTING
;; ;; ============

;; (rf/reg-event-fx
;;  ::start-broadcast-button-clicked
;;  (fn [_ _]
;;    {::fx/request-user-media
;;     {:on-ready [::user-media-ready]
;;      :on-error [::error-occurred "Could not access webcam and microphone."]}}))

;; (rf/reg-event-fx
;;  ::user-media-ready
;;  (fn [_ _]
;;    {::fx/start-broadcasting {:on-ready [::broadcasting-ready]}}))

;; (rf/reg-event-fx
;;  ::broadcasting-ready
;;  (fn [{:keys [db]} [_ {:keys [video-producer-id audio-producer-id]}]]
;;    (let [{:connection/keys [full-jid]
;;           :chats/keys [chats active-chat-jid]}
;;          db]
;;      {:db (assoc db
;;                  :media/broadcasting? true
;;                  :media/video-producer-id video-producer-id
;;                  :media/audio-producer-id audio-producer-id)
;;       :xmpp/send
;;       {:stanza (media-presence-stanza
;;                 {:from full-jid
;;                  :to (get-in chats [active-chat-jid :chat/from-jid])
;;                  :video-producer-id video-producer-id
;;                  :audio-producer-id audio-producer-id})}})))
