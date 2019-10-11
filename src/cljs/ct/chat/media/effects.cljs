(ns ct.chat.media.effects
  (:require
   [cljs.core.async :as async :refer [go <! alt!]]
   [re-frame.core :as rf]
   [ct.chat.media.device
    ]))

;; (rf/reg-fx
;;  ::load-device
;;  (fn [{:keys [capabilities on-loaded]}]
;;    (go
;;      (<! (load-device! capabilities))
;;      (rf/dispatch on-loaded))))

;; (rf/reg-fx
;;  ::create-receive-transport
;;  (fn [{:keys [parameters]}]
;;    (create-receive-transport! parameters)))

;; (rf/reg-fx
;;  ::create-send-transport
;;  (fn [{:keys [parameters]}]
;;    (create-send-transport! parameters)))

;; (rf/reg-fx
;;  ::request-user-media
;;  (fn [{:keys [on-success on-error timeout on-timeout]
;;        :or {timeout 15000
;;             on-timeout [::request-user-media.timeout]}}]
;;    (go
;;      (let [timeout-ch (async/timeout timeout)]
;;        (rf/dispatch
;;         (alt!
;;           (request-user-media!)
;;           ([result]
;;            (if (= result :ok)
;;              on-success
;;              on-error))
;;           timeout-ch on-error))))))

;; (rf/reg-fx
;;  ::broadcast-webcam
;;  (fn [{:keys [on-ready]}]
;;    (go
;;      (let [producer-id (<! (broadcast-webcam!))]
;;        (rf/dispatch (conj on-ready producer-id))))))

;; (rf/reg-fx
;;  ::create-consumer
;;  (fn [{:keys [parameters on-ready on-track-ended]}]
;;    (go
;;      (<! (create-consumer!
;;           parameters
;;           {:on-track-ended #(rf/dispatch on-track-ended)}))
;;      (rf/dispatch on-ready))))
