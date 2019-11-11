(ns ct.chat.media.effects
  (:require
   [cljs.core.async :as async :refer [go <! alt!]]
   [re-frame.core :as rf]
   [ct.chat.media.connection :refer [reset-connection!]]
   [ct.chat.media.device
    :refer [request-user-media!
            start-broadcasting!
            start-consuming!
            consume!]]))

(rf/reg-fx
 ::connect
 (fn [{:keys [url on-open on-failed on-disconnected on-close]}]
   (reset-connection! url {:on-open #(rf/dispatch on-open)
                           :on-failed #(rf/dispatch on-failed)
                           :on-disconnected #(rf/dispatch on-disconnected)
                           :on-close #(rf/dispatch on-close)})))

(rf/reg-fx
 ::request-user-media
 (fn [{:keys [on-ready on-error]}]
   (go
     (if (<! (request-user-media!))
       (rf/dispatch on-ready)
       (rf/dispatch on-error)))))

(rf/reg-fx
 ::start-broadcasting
 (fn [{:keys [on-ready]}]
   (go
     (rf/dispatch (conj on-ready (<! (start-broadcasting!)))))))

(rf/reg-fx
 ::start-consuming
 (fn [_]
   (start-consuming!)))
