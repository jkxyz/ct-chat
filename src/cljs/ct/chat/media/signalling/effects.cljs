(ns ct.chat.media.signalling.effects
  (:require
   [cljs.core.async :as async :refer [go <!]]
   [re-frame.core :as rf]
   [ct.chat.media.signalling.peer :as peer]))

(rf/reg-fx
 ::initialize
 (fn [{:keys [url]}]
   (peer/initialize! {:url url})))

(rf/reg-fx
 ::set-notification-handlers
 (fn [handlers]
   (doseq [[method event] handlers]
     (peer/set-notification-handler! method #(rf/dispatch (conj event %))))))

(rf/reg-fx
 ::request
 (fn [{:keys [method data on-success on-reject]
       :or {on-reject [::request.rejected]}}]
   (go
     (let [response (<! (peer/request! method data))]
       (if-not (= response ::peer/rejected)
         (rf/dispatch (conj on-success response))
         (rf/dispatch on-reject))))))
