(ns ct.chat.media.subscriptions
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 ::broadcasting?
 (fn [{:media/keys [broadcasting?]}]
   broadcasting?))
