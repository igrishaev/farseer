(ns farseer.handler
  (:require
   [farseer.spec.handler :as handler]

   [clojure.tools.logging :as log]

   [clojure.spec.alpha :as s]))


;; make name

;; check config with spec

;; check functions in config
;; auto-load func ns in config

;; better explain (expound?)
;; rename version & opt-un

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


(def rpc-errors
  {:parse-error
   {:status 500
    :code -32700
    :message "Parse error"}

   :invalid-request
   {:status 400
    :code -32600
    :message "Invalid Request"}

   :not-found
   {:status 404
    :code -32601
    :message "Method not found"}

   :invalid-params
   {:status 400
    :code -32602
    :message "Invalid params"}

   :internal-error
   {:status 500
    :code -32603
    :message "Internal error"}})


(def into-map (partial into {}))


(def code->status
  (into-map
   (map (juxt :code :status)
        (vals rpc-errors))))


(defn rpc-error!
  [{:as params :keys [type]}]

  (let [rpc-error
        (or (get rpc-errors type)
            (get rpc-errors :internal-error))

        data
        (merge rpc-error params)]

    (throw (ex-info "RPC error" data))))


(defn find-method
  [{:as this :keys [config rpc]}]

  (let [{:keys [method]} rpc
        {:keys [handlers]} config

        handler-map (get handlers method)]

    (if-not handler-map
      (rpc-error! {:type :not-found})
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
      (rpc-error! {:type :invalid-params
                   :data {:explain explain}})
      this)))


(defn execute-method
  [{:as this :keys [context config rpc handler-map]}]

  (let [{:keys [params]} rpc
        {:keys [handler]} handler-map

        ;; TODO assert handler (symbol)

        arg-list
        (if (vector? params)
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
      ;; log?
      (rpc-error! {:type :internal-error})
      this)))


(defn compose-response
  [{:as this :keys [rpc result]}]
  (let [{:keys [id version]} rpc]
    (when id
      {:id id
       :jsonrpc version
       :result result})))


(def types-no-log
  #{:invalid-request
    :not-found
    :invalid-params})


(defn rpc-error-handler
  [this e]

  (let [{:keys [rpc]} this
        {:keys [id method]} rpc

        err-data (ex-data e)

        {:keys [type]} err-data

        rpc-error
        (or
         (get rpc-errors type)
         (get rpc-errors :internal-error))

        rpc-error
        (merge rpc-error err-data)

        {:keys [code message data]}
        rpc-error

        data
        (assoc data :method method)]

    (when-not (contains? types-no-log type)
      ;; log method, id, etc
      (log/error e))

    {:id id
     :jsonrpc "2.0"
     :error {:code code
             :message message
             :data data}}))


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


(defn guess-http-status
  [{:keys [error]}]
  (if error
    (let [{:keys [code]} error]
      (get code->status code 500))
    200))


(defn check-batch-limit
  [{:as this :keys [config rpc]}]

  (let [{:keys [max-batch-size]} config

        batch-size (count rpc)

        exeeded?
        (when max-batch-size
          (> batch-size max-batch-size))]

    (if exeeded?
      (rpc-error! {:type :invalid-params
                   :message "Batch size is too large"})
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
      check-batch-limit
      execute-batch))


(defn step-1-parse-payload
  [{:as this :keys [config request]}]

  (let [{:keys [data-field]} config

        rpc
        (get request data-field)

        explain
        (explain-str ::handler/rpc rpc)

        batch?
        (when-not explain
          (vector? rpc))]

    (if explain

      (rpc-error! {:type :invalid-request
                   :data {:explain explain}})

      (assoc this
             :rpc rpc
             :batch? batch?))))


(defn step-2-check-batch
  [{:as this :keys [config batch?]}]

  (let [{:keys [allow-batch?]} config]

    (if (and batch? (not allow-batch?))
      (rpc-error! {:type :invalid-request})
      this)))


(defn step-3-process-rpc
  [{:as this :keys [batch?]}]

  (let [result
        (if batch?
          (process-rpc-batch this)
          (process-rpc-single this))]

    (assoc this :result result)))


(defn step-4-http-response
  [{:as this :keys [batch? result]}]

  (if batch?

    (let [result (remove nil? result)]
      {:status 200
       :body result})

    (let [status (guess-http-status result)]
      {:status status
       :body result})))


(def config-default
  {:data-field :params
   :validate-in-spec? true
   :validate-out-spec? true
   :allow-batch? true
   :max-batch-size 25
   :parallel-batch? true})


(defn make-handler

  ([config]
   (make-handler config nil))

  ([config context]

   (let [config
         (merge config-default config)]

     (fn [request]

       (let [context
             (assoc context :request request)

             this
             {:config config
              :request request
              :context context}]

         (-> this

             step-1-parse-payload
             step-2-check-batch
             step-3-process-rpc
             step-4-http-response

             (with-try [e]
               (let [result (rpc-error-handler this e)
                     status (guess-http-status result)]
                 {:status status
                  :body result}))))))))


;;;;;;;;;



#_
(defn ensure-fn [obj]
  (cond

    (fn? obj)
    obj

    (and (var? obj) (fn? @obj))
    obj

    (and (symbol? obj) (resolve obj))
    (resolve obj)

    :else
    (throw (new Exception (format "Wrong function: %s" obj)))))