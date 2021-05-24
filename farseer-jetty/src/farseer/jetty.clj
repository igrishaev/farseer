(ns farseer.jetty
  (:require
   [farseer.config :as config]
   [farseer.http :as http]
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
         (->> defaults
              (config/rebase config)
              (s/assert ::spec.jetty/config))

         app
         (http/make-app config context)

         jetty-opt
         (config/query-keys config "jetty")]

     (run-jetty app jetty-opt))))


(defn stop-server [server]
  (.stop ^Server server))


(defmacro with-server
  [[config & [context]] & body]
  `(let [server# (start-server ~config ~context)]
     (try
       ~@body
       (finally
         (stop-server server#)))))


(defn component

  ([config]
   (component config nil))

  ([config context]

   (with-meta context

     {'com.stuartsierra.component/start
      (fn [{:as this :jetty/keys [server]}]
        (if server
          this
          (let [server (start-server config this)]
            (assoc this :jetty/server server))))

      'com.stuartsierra.component/stop
      (fn [{:as this :jetty/keys [server]}]
        (when server
          (stop-server server)
          (dissoc this :jetty/server)))})))
