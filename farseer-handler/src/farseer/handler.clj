(ns farseer.handler
  (:require
   [farseer.spec.handler :as handler]

   [clojure.tools.logging :as log]

   [clojure.spec.alpha :as s]))


;; make name
;; init multiproject

;; check specs in config
;; check functions in config
;; auto-load func ns in config

;; refactor errors
;; better explain (expound?)
;; drop version

;; method overrides

;; spec for config
;; validate config in handler

;; hadnler overrides
;; discover handler
;; rename fucntions


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
  [params]

  (let [{:keys [type]} params

        rpc-error
        (or (get rpc-errors type)
            (get rpc-errors :internal-error))

        data
        (merge rpc-error params)]

    (throw (ex-info "RPC error" data))))


(defn find-method
  [{:as this :keys [config rpc]}]

  (let [{:keys [id method]} rpc
        {:keys [handlers]} config

        handler-map (get handlers method)]

    (if-not handler-map
      (rpc-error! {:id id
                   :type :not-found
                   :data {:method method}})
      (assoc this
             :handler-map handler-map))))


(defn validate-params
  [{:as this :keys [config rpc handler-map]}]

  (let [{:keys [id method params]}
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
      (rpc-error! {:id id
                   :type :invalid-params
                   :data {:method method
                          :explain explain}})
      this)))


(defn execute-method
  [{:as this :keys [config request rpc handler-map]}]

  (let [{:keys [params]} rpc
        {:keys [handler]} handler-map

        ;; TODO assert handler (symbol)

        {:keys [pass-request-to-handler?]}
        config

        arg-list
        (if (vector? params)
          params [params])

        arg-list
        (if pass-request-to-handler?
          (cons request arg-list)
          arg-list)

        result
        (apply handler arg-list)]

    (assoc this :result result)))


(defn validate-output
  [{:as this :keys [config result rpc handler-map]}]

  (let [{:keys [spec-out]} handler-map

        {:keys [validate-out-spec?]} config

        {:keys [id method]} rpc

        validate?
        (and validate-out-spec? spec-out)

        explain
        (when validate?
          (explain-str spec-out result))]

    (if explain
      ;; TODO log

      (rpc-error! {:id id
                   :type :internal-error
                   :data {:method method}})

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


(defn rpc-single-error-handler
  [e]
  (let [{:keys [id code message data]}
        (ex-data e)]

    (when-not (contains? types-no-log type)
      (log/error e))

    {:id id
     :jsonrpc "2.0"
     :error {:code code
             :message message
             :data data}}))


(defn coerce-rpc
  [this]
  (update-in this [:rpc :method] keyword))


(defn process-rpc-single
  [this]
  (-> this
      coerce-rpc
      find-method
      validate-params
      execute-method
      validate-output
      compose-response
      (try
        (catch Throwable e
          (rpc-single-error-handler e)))))


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

        {:keys [id]} rpc

        exeeded?
        (when max-batch-size
          (> batch-size max-batch-size))]

    (if exeeded?
      (rpc-error! {:id id
                   :type :invalid-params
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
  {:pass-request-to-handler? true
   :data-field :params
   :validate-in-spec? true
   :validate-out-spec? true
   :allow-batch? true
   :max-batch-size 25
   :parallel-batch? true})


(defn make-handler
  [config]
  (fn [request]

    (-> {:config (merge config-default config)
         :request request}

        step-1-parse-payload
        step-2-check-batch
        step-3-process-rpc
        step-4-http-response

        (try
          (catch Throwable e
            (log/error e)

            (let [{:keys [status code message data]}
                  (ex-data e)]

              {:status (or status 500)
               :body {:error {:code code
                              :message message
                              :data data}}}))))))



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
