(ns ct.chat.media.subscriptions
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 ::consumer-ids
 (fn [db]
   (keys (:media/consumers db))))

(rf/reg-sub
 ::broadcast-state
 (fn [db]
   (:media/broadcast-state db)))
