(ns farseer.server.jetty
  (:require
   [farseer.server.http :as http]

   [ring.adapter.jetty :refer [run-jetty]])

  (:import
   org.eclipse.jetty.server.Server))


(def config-default
  {:jetty/port 8080
   :jetty/join? false})


(defn get-keys [config ns]
  (persistent!
   (reduce-kv
    (fn [result k v]
      (if (= (namespace k) (name ns))
        (assoc! result (keyword (name k)) v)
        result))
    (transient {})
    config)))


(defn ^Server start-server

  ([config]
   (start-server config nil))

  ([config context]

   (let [config
         (merge config-default config)

         app
         (http/make-app config context)

         jetty-opt
         (get-keys config "jetty")]

     (run-jetty app jetty-opt))))


;; component?


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
