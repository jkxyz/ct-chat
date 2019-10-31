(ns ct.chat.media.subscriptions
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 ::broadcasting?
 (fn [{:media/keys [broadcasting?]}]
   broadcasting?))

(rf/reg-sub
 ::video-producers
 (fn [{:media/keys [producers]}]
   (->> (mapcat val producers)
        (filter #(= :video (:producer/kind %))))))
