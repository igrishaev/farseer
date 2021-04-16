(ns farseer.server.jetty
  (:require
   [farseer.config :as config]
   [farseer.server.http :as http]
   [farseer.spec.jetty :as spec.jetty]

   [ring.adapter.jetty :refer [run-jetty]]

   [clojure.spec.alpha :as s])

  (:import
   org.eclipse.jetty.server.Server))


(def defaults
  {:jetty/port 8080
   :jetty/join? false})


(defn ^Server start-server

  ([config]
   (start-server config nil))

  ([config context]

   (let [config
         (config/add-defaults config defaults)]

     (s/assert ::spec.jetty/config config)

     (let [app
           (http/make-app config context)

           jetty-opt
           (config/query-keys config "jetty")]

       (run-jetty app jetty-opt)))))


(defn stop-server [^Server server]
  (.stop server))


(defn component [config]

  (with-meta {}

    {'com.stuartsierra.component/start
     (fn [{:as this :keys [server]}]
       (if server
         this
         (let [server (start-server config this)]
           (assoc this :server server))))

     'com.stuartsierra.component/stop
     (fn [{:as this :keys [server]}]
       (when server
         (stop-server server)
         (dissoc this :server)))}))
