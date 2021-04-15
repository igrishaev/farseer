(ns farseer.server.jetty
  (:require
   [farseer.server.http :as http]

   [ring.adapter.jetty :refer [run-jetty]]))


(def config-default
  {:jetty/port 8080
   :jetty/join? false})


(defn start-server

  ([config]
   (start-server config nil))

  ([config context]

   (let [config
         (merge config-default config)

         app
         (http/make-app config context)

         {:jetty/keys [port join?]}
         config]

     (run-jetty app {:port port
                     :join? join?}))))
