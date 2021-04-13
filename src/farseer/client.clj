(ns farseer.client
  (:require
   [clj-http.client :as client]

   ;; [clojure.string :as str]
   ;; [clojure.spec.alpha :as s]
   ))


(defn generate-id [fn-id]
  42
  #_
  (cond
    (= fn-id :int)
    (rand-int 99999)

    (= fn-id :uuid)
    (str (java.util.UUID/randomUUID))

    (fn? fn-id)
    (fn-id)

    :else
    (throw (ex-info "Wrong ID function" {:fn-id fn-id}))))



(def config-defaults
  {
   :rpc/fn-before-send identity
   :rpc/fn-id          :int

   ;; :rpc/multi? false
   ;; :rpc/fn-dispatch-service get-ns
   ;; :rpc/notify? false

   :method             :post
   :url                "http://127.0.0.1:8008/api"
   :headers            {:user-agent "jsonrpc.client"}
   :socket-timeout     5000
   :connection-timeout 5000
   :throw-exceptions?  false
   :as                 :json
   :content-type       :json
   :coerce             :always

   ;; conn manager


   })


(defn make-config [config]
  ;; todo deep merge
  (merge config-defaults config))


(defn rebase-config [config method]
  (let [method-options
        (get-in config [:method-options method])]
    (merge config method-options)))


(defn make-payload [config method params]
  (let [config
        (rebase-config config method)

        {:rpc/keys [fn-id
                    fn-before-send
                    notify?]}
        config

        id
        (when-not notify?
          (generate-id fn-id))]

    (cond-> {:version "2.0" ;; todo
             :method method}
      id
      (assoc :id id)

      params
      (assoc :params params))))


(defn make-request [config payload]

  (let [{:rpc/keys [fn-before-send]}
        config

        request
        (-> config
            (assoc :form-params payload))]

    (-> request
        fn-before-send
        client/request
        ;; :body
        ;; :result

        )))


(defn rpc-inner

  ([config method]
   (rpc-inner config method nil))

  ([config method params]

   (let [payload
         (make-payload config method params)]

     (make-request config payload))))


(defn call

  ([config method]
   (rpc-inner config method))

  ([config method params]
   (if (map? params)
     (rpc-inner config method params)
     (rpc-inner config method [params])))

  ([config method arg & args]
   (rpc-inner config method (cons arg args))))


(defn with-notify [config method]
  (assoc-in
   config
   [:method-options method :rpc/notify?]
   true))


(defn notify

  ([config method]
   (call (with-notify config method) method))

  ([config method params]
   (call (with-notify config method) method params))

  ([config method arg & args]
   (apply call (with-notify config method) method arg args)))


;; TODO rebase config once!
;; not json params but json body

(defn batch [config batches]

  (let [payload
        (for [[method params] batches]
          (make-payload config method params))]

    (make-request config payload)))



;; batch
;; multi-client
;; multi-resolve
;; conn-manager
;; as component




;; (defn get-ns [kw]
;;   (-> kw namespace keyword))





;; (def config-multi
;;   {:rpc/multi? true
;;    :rpc/services
;;    {:users
;;     {:url "http://ccc.com/aaa"
;;      :auth ["fff" "ggg"]}
;;     :sales
;;     {:url "http://bbb.com/ccc"
;;      :auth ["fff" "ggg"]}}})








;; (defn batch [client batches]

;;   )


;; (defn call-batch
;;   [config batches]

;;   (rpc-inner config (remap-batches batches))






;;   )
