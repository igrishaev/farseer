(ns farseer.server.jetty
  (:require
   [farseer.config :as config]
   [farseer.server.http :as http]

   [ring.adapter.jetty :refer [run-jetty]])

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
         (config/add-defaults config defaults)

         app
         (http/make-app config context)

         jetty-opt
         (config/query-keys config "jetty")]

     (run-jetty app jetty-opt))))


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
         (.stop ^Server server)
         (dissoc this :server)))}))
