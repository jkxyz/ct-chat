(ns ^:figwheel-no-load ct.chat.dev
  (:require
    [ct.chat.core :as core]
    [devtools.core :as devtools]))

(devtools/install!)

(enable-console-print!)

(core/init!)
