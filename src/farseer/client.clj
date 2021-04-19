(ns farseer.client
  (:require
   [farseer.config :as config]
   [farseer.spec.rpc :as spec.rpc]
   [farseer.spec.client :as spec.client]

   [clj-http.client :as client]
   [clj-http.conn-mgr :as conn-mgr]

   [clojure.tools.logging :as log]
   [clojure.spec.alpha :as s]))


(defn generate-id [fn-id]
  (cond
    (= fn-id :id/int)
    (rand-int 99999)

    (= fn-id :id/uuid)
    (str (java.util.UUID/randomUUID))

    (fn? fn-id)
    (fn-id)

    :else
    (throw (ex-info "Wrong ID function" {:fn-id fn-id}))))



(def defaults
  {:rpc/fn-before-send identity
   :rpc/fn-id          :id/int

   :http/method             :post
   :http/headers            {:user-agent "farseer.client"}
   :http/socket-timeout     5000
   :http/connection-timeout 5000
   :http/as                 :json
   :http/content-type       :json
   :http/throw-exceptions?  false
   :http/coerce             :always
   :http/connection-manager nil

   :conn-mgr/timeout           5
   :conn-mgr/threads           4
   :conn-mgr/default-per-route 2
   :conn-mgr/insecure?         false})


(defn make-client [config]
  (->> (config/add-defaults config defaults)
       (s/assert ::spec.client/config)))


(defn start-conn-mgr [config]
  (let [conn-mgr-opt
        (config/query-keys config "conn-mgr")

        conn-mgr
        (conn-mgr/make-reusable-conn-manager conn-mgr-opt)]

    (assoc config :http/connection-manager conn-mgr)))


(defn stop-conn-mgr
  [{:as config :http/keys [connection-manager]}]

  (when connection-manager
    (conn-mgr/shutdown-manager connection-manager))

  (assoc config :http/connection-manager nil))


(defmacro with-conn-mgr
  [[bind config] & body]
  `(let [~bind (start-conn-mgr ~config)]
     (try
       ~@body
       (finally
         (stop-conn-mgr ~bind)))))


(defn component [config]

  (with-meta (make-client config)

    {'com.stuartsierra.component/start
     (fn [this]
       (start-conn-mgr this))

     'com.stuartsierra.component/stop
     (fn [this]
       (stop-conn-mgr this))}))


(defn make-request [config payload]

  (s/assert ::spec.rpc/rpc payload)

  (log/debugf "RPC call: %s" payload)

  (let [{:rpc/keys [fn-before-send]}
        config

        ;; TODO: prepare http/client keys
        request
        (-> config
            (config/query-keys "http")
            (assoc :form-params payload))]

    (-> request
        fn-before-send
        client/request
        :body)))


(defn make-payload

  ([config method params]
   (make-payload config method params nil))

  ([config method params options]

   (let [{:rpc/keys [notify]}
         options

         {:rpc/keys [fn-id]}
         config

         id
         (when-not notify
           (generate-id fn-id))]

     (cond-> {:jsonrpc "2.0"
              :method method}

       id
       (assoc :id id)

       params
       (assoc :params params)))))


(defn call

  ([config method]
   (call config method nil))

  ([config method params]
   (let [payload
         (make-payload config method params)]
     (make-request config payload))))


(defn notify

  ([config method]
   (notify config method nil))

  ([config method params]
   (let [payload
         (make-payload config method params
                       {:rpc/notify true})]
     (make-request config payload))))


(defn batches->payload [config batches]
  (reduce
   (fn [result batch]
     (let [[method params] batch
           notify (some-> batch meta :rpc/notify)
           payload (make-payload config method params
                                 {:rpc/notify notify})]
       (conj result payload)))
   []
   batches))


(defn batch [config batches]
  (let [payload (batches->payload config batches)]
    (make-request config payload)))
