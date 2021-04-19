(ns farseer.handler
  (:require
   [farseer.config :as config]
   [farseer.error :as e]

   [farseer.spec.handler :as spec.handler]
   [farseer.spec.rpc :as spec.rpc]

   [clojure.tools.logging :as log]

   [clojure.spec.alpha :as s]))


;; review sequential?

;; make name

;; check config with spec

;; check functions in config
;; auto-load func ns in config

;; better explain (expound?)

;; method overrides

;; spec for config
;; validate config in handler

;; handler overrides
;; discover handler
;; rename functions

;; no-log field maybe?


(defn explain-str [spec data]
  (let [out (s/explain-str spec data)]
    (when-not (= out "Success!\n")
      out)))


(defn find-method
  [{:as this :keys [config rpc]}]

  (let [{:keys [method]} rpc
        {:keys [rpc/handlers]} config

        handler (get handlers method)]

    (if-not handler
      (e/not-found! {:rpc/data {:method method}})
      (assoc this :handler handler))))


(defn validate-params
  [{:as this :keys [config rpc handler]}]

  (let [{:keys [params]}
        rpc

        {:rpc/keys [spec-validate-in?]}
        config

        {:handler/keys [spec-in
                        spec-out
                        function]} handler

        validate?
        (and spec-validate-in? spec-in)

        explain
        (when validate?
          (explain-str spec-in params))]

    (if explain
      (e/invalid-params! {:rpc/data {:explain explain}})
      this)))


(defn execute-method
  [{:as this :keys [context rpc handler]}]

  (let [{:keys [id method params]} rpc
        {:handler/keys [function]} handler

        context
        (assoc context :id id :method method)

        ;; TODO assert handler (symbol)

        arg-list
        (if (sequential? params)
          params [params])

        ;; todo: drop apply maybe?
        result
        (apply function context arg-list)]

    (assoc this :result result)))


(defn validate-output
  [{:as this :keys [config result handler]}]

  (let [{:handler/keys [spec-out]} handler

        {:rpc/keys [spec-validate-out?]}
        config

        validate?
        (and spec-validate-out? spec-out)

        explain
        (when validate?
          (explain-str spec-out result))]

    (if explain

      (do
        (log/error explain)
        (e/internal-error!))

      this)))


(defn compose-response
  [{:as this :keys [rpc result]}]
  (let [{:keys [id jsonrpc]} rpc]
    (when id
      {:id id
       :jsonrpc jsonrpc
       :result result})))


(defn rpc-error-handler
  [{:as this :keys [rpc]} e]

  (let [response (e/->response e)

        {:keys [id method jsonrpc]}
        rpc]

    (-> response
        (assoc :id id)
        (assoc :jsonrpc jsonrpc)
        (assoc-in [:error :data :method] method))))


(defn coerce-rpc
  [this]
  (update-in this [:rpc :method] keyword))


(defmacro with-try
  [x [e] & body]
  `(try
     ~x
     (catch Throwable ~e
       ~@body)))


(defn process-rpc-single
  [this]
  (-> this
      coerce-rpc
      find-method
      validate-params
      execute-method
      validate-output
      compose-response
      (with-try [e]
        (rpc-error-handler this e))))


(defn check-batch-limit
  [{:as this :keys [config rpc]}]

  (let [{:rpc/keys [batch-max-size]} config

        batch-size (count rpc)

        exeeded?
        (when batch-max-size
          (> batch-size batch-max-size))]

    (if exeeded?
      (e/invalid-params
       {:rpc/message "Batch size is too large"})
      this)))


(defn execute-batch
  [{:as this :keys [config rpc]}]

  (let [{:rpc/keys [batch-parallel?]} config

        fn-map
        (if batch-parallel? pmap map)

        fn-single
        (fn [rpc-single]
          (process-rpc-single
             (assoc this :rpc rpc-single)))]

    (fn-map fn-single rpc)))


(defn process-rpc-batch
  [this]
  (-> this
      (check-batch-limit)
      (execute-batch)))


(defn step-1-parse-payload
  [{:as this :keys [rpc]}]

  (let [explain
        (explain-str ::spec.rpc/rpc rpc)

        batch?
        (when-not explain
          (sequential? rpc))]

    (if explain

      (e/invalid-params
       {:rpc/data {:explain explain}})

      (assoc this
             :rpc rpc
             :batch? batch?))))


(defn step-2-check-batch
  [{:as this :keys [config batch?]}]

  (let [{:rpc/keys [batch-allowed?]} config]

    (if (and batch? (not batch-allowed?))

      (e/invalid-params
       {:rpc/message "Batch is not allowed"})

      this)))


(defn step-3-process-rpc
  [{:as this :keys [batch?]}]

  (if batch?

    (->> this
         (process-rpc-batch)
         (remove nil?))

    (process-rpc-single this)))


(def defaults
  {:rpc/spec-validate-in? true
   :rpc/spec-validate-out? true
   :rpc/batch-allowed? true
   :rpc/batch-max-size 25
   :rpc/batch-parallel? true})


(defn make-handler

  ([config]
   (make-handler config nil))

  ([config context]

   (let [config
         (config/add-defaults config defaults)]

     (s/assert ::spec.handler/config config)

     (fn rpc-handler

       ([rpc]
        (rpc-handler rpc nil))

       ([rpc context-local]

        (-> {:config config
             :context (merge context context-local)
             :rpc rpc}

            step-1-parse-payload
            step-2-check-batch
            step-3-process-rpc

            (with-try [e]
              ;; todo no log
              (log/error e)
              (e/->response e))))))))
