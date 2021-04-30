(ns farseer.handler
  (:require
   [farseer.config :as config]
   [farseer.error :as e]

   [farseer.spec.handler :as spec.handler]
   [farseer.spec.rpc :as spec.rpc]

   [clojure.tools.logging :as log]

   [clojure.spec.alpha :as s]))


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
        (assoc context :rpc/id id :rpc/method method)

        result
        (function context params)]

    (assoc this :result result)))


(defn validate-output
  [{:as this :keys [config result handler rpc]}]

  (let [{:keys [id method]} rpc

        {:handler/keys [spec-out]} handler

        {:rpc/keys [spec-validate-out?]}
        config

        validate?
        (and spec-validate-out? spec-out)

        explain
        (when validate?
          (explain-str spec-out result))]

    (if explain
      (throw
       (ex-info
        "RPC result doesn't match the output spec"
        {:explain explain}))
      this)))


(defn compose-response
  [{:as this :keys [rpc result]}]
  (let [{:keys [id jsonrpc]} rpc]
    (when id
      {:id id
       :jsonrpc jsonrpc
       :result result})))


(defn rpc-error-handler

  ([e]
   (rpc-error-handler e nil))

  ([e rpc]

   (let [exception-data
         (merge e/internal-error (ex-data e))

         {:rpc/keys [code message data]
          :log/keys [level stacktrace?]}
         exception-data

         {:keys [id method jsonrpc]}
         rpc

         response
         {:error {:code code
                  :message message}}

         report
         (format "%s, id: %s, method: %s, code: %s, message: %s"
                 (ex-message e) id method code message)]

     (if stacktrace?
       (log/log level e report)
       (log/log level report))

     (cond-> response

       data
       (assoc-in [:error :data] data)

       id
       (assoc :id id)

       jsonrpc
       (assoc :jsonrpc jsonrpc)

       method
       (assoc-in [:error :data :method] method)))))


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
  [{:as this :keys [rpc]}]
  (-> this
      coerce-rpc
      find-method
      validate-params
      execute-method
      validate-output
      compose-response
      (with-try [e]
        (rpc-error-handler e rpc))))


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
         (->> defaults
              (config/rebase config)
              (s/assert ::spec.handler/config))]

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
              (rpc-error-handler e))))))))
