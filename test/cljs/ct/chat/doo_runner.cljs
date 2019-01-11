(ns ct.chat.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [ct.chat.core-test]))

(doo-tests 'ct.chat.core-test)
