(ns farseer.handler
  (:require
   [farseer.error :as e]
   [farseer.spec.handler :as handler]

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
        {:keys [handlers]} config

        handler-map (get handlers method)]

    (if-not handler-map
      (e/not-found! {:rpc/data {:method method}})
      (assoc this :handler-map handler-map))))


(defn validate-params
  [{:as this :keys [config rpc handler-map]}]

  (let [{:keys [params]}
        rpc

        {:keys [validate-in-spec?]}
        config

        {:keys [spec-in
                spec-out
                handler]} handler-map

        validate?
        (and validate-in-spec? spec-in)

        explain
        (when validate?
          (explain-str spec-in params))]

    (if explain
      (e/invalid-params! {:rpc/data {:explain explain}})
      this)))


(defn execute-method
  [{:as this :keys [context rpc handler-map]}]

  (let [{:keys [params]} rpc
        {:keys [handler]} handler-map

        ;; TODO assert handler (symbol)

        arg-list
        (if (sequential? params)
          params [params])

        result
        (apply handler context arg-list)]

    (assoc this :result result)))


(defn validate-output
  [{:as this :keys [config result handler-map]}]

  (let [{:keys [spec-out]} handler-map

        {:keys [validate-out-spec?]} config

        validate?
        (and validate-out-spec? spec-out)

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

  (let [{:keys [max-batch-size]} config

        batch-size (count rpc)

        exeeded?
        (when max-batch-size
          (> batch-size max-batch-size))]

    (if exeeded?
      (e/invalid-params
       {:rpc/message "Batch size is too large"})
      this)))


(defn execute-batch
  [{:as this :keys [config rpc]}]

  (let [{:keys [parallel-batch?]} config

        fn-map
        (if parallel-batch? pmap map)

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
        (explain-str ::handler/rpc rpc)

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

  (let [{:keys [allow-batch?]} config]

    (if (and batch? (not allow-batch?))

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


(def config-default
  {:validate-in-spec? true
   :validate-out-spec? true
   :allow-batch? true
   :max-batch-size 25
   :parallel-batch? true})


(defn make-handler

  ([config]
   (make-handler config nil))

  ([config globals]

   (fn rpc-handler

     ([rpc]
      (rpc-handler rpc nil))

     ([rpc locals]

      (-> {:config (merge config-default config)
           :context (merge globals locals)
           :rpc rpc}

          step-1-parse-payload
          step-2-check-batch
          step-3-process-rpc

          (with-try [e]
            (e/->response e)))))))
