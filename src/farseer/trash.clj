(defn wrap-rpc-auth
  [rpc-handler]
  (fn [rpc {:as locals :keys [user]}]
    (if user
      (rpc-handler rpc locals)
      {:non-auth :request})))

(defn authenticate [request]
  {:id 1 :name "Ivan"})

(defn make-http-app
  [config]

  (let [context {:db {:host "127.0.0.1"}}

        handler (-> (make-handler config context)
                    (wrap-rpc-auth))]

    (fn [{:as request :keys [method uri body]}]

      (if (and (= :post method) (= "/api" uri))

        (let [user (authenticate request)]

          {:status 200
           :body (handler body {:user user})})

        {:status 404 :body {:not :found}}))))


(defprotocol RPCHandler

  (spec-in [this]
    )

  (spec-out [this]
    )

  (handle [this params]))


(defrecord GetUserByID
    [db]

  IRPCHandler

  (handle [this [id]]
    (jdbc/query db ["select * from users where id=?" id])))


(defn make-http-app [config fn-req->context]

  (let [handler (make-http-handler config)]

    (fn [{:as request :keys [method uri]} foo]

      (if (and (= :post method) (= "/api" uri))
        (handler request (fn-context request))

        {:not :found}))))


(defn make-ws-handler
  [config]

  (fn [request]

    (let [user (authenticate request)
          ctx {:db :database :user user}
          rpc-handler (make-handler config ctx)]

      (http/websocket-connection req)

      (rpc-handler)))

  #_
  (let [context     {:db {:host "127.0.0.1"}}
        rpc-handler (make-handler config context)]

    (fn [{:as request :keys [body]}]

      (let [user (authenticate request)]

        (rpc-handler body {:user user})))))


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
