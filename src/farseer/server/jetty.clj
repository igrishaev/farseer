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


;; component?


(defn component [config]

  (with-meta {}

    {'foo.bar/start
     (fn [{:as this :keys [server]}]
       ;;;
       (let [context this
             server (start-server config context)]
         (assoc this :server server)))

     'foo.bar/stop
     (fn [{:as this :keys [server]}]
       (when server
         (.close server)
         (dissoc this :server)))}))
