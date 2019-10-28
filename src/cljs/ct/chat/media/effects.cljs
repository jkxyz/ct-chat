(ns ct.chat.media.effects
  (:require
   [cljs.core.async :as async :refer [go <! alt!]]
   [re-frame.core :as rf]
   [ct.chat.media.device
    :refer [request-user-media!
            start-broadcasting!]]))

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
