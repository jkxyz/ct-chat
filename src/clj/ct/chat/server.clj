(ns ct.chat.server
  (:gen-class)
  (:require
   [ct.chat.handler :refer [app]]
   [config.core :refer [env]]
   [ring.adapter.jetty :refer [run-jetty]]))

(defn -main [& args]
  (let [port (or (env :port) 3000)]
    (run-jetty app {:port port :join? false})))
