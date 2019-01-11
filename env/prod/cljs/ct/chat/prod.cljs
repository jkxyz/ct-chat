(ns ct.chat.prod
  (:require [ct.chat.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
