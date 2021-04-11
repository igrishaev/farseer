(ns farseer.stub
  (:require
   [farseer.handler :as handler]

   [clojure.tools.logging :as log]

   [ring.adapter.jetty :refer [run-jetty]]

   [ring.middleware.params
    :refer [wrap-params]]

   [ring.middleware.keyword-params
    :refer [wrap-keyword-params]]

   [ring.middleware.json
    :refer [wrap-json-params
            wrap-json-response]]))


(def config-default
  {:port 8008
   :path "/api"
   :http-method :post})


(defn remap-handlers
  [method->response]
  (reduce-kv
   (fn [result method response]
     (assoc-in result
               [method :handler]
               (fn [& _]
                 response)))
   {}
   method->response))


(defn make-handler [config]
  (-> config
      (update :handlers remap-handlers)
      handler/make-handler))


(defn make-app
  [config]

  (let [handler (make-handler config)]

    (fn [{:as request
          :keys [request-method uri]}]

      (if (and
           (= request-method :post)
           (= uri "/api"))

        (handler request)

        {:status 404
         :body {:foo 42}}))))


(defn wrap-app
  [app]
  (-> app
      wrap-keyword-params
      wrap-json-params
      wrap-params
      wrap-json-response))


(defmacro with-stub
  [config & body]

  `(let [config#
         (merge config-default ~config)

         {port# :port} config#

         app#
         (-> config#
             make-app
             wrap-app)

         server#
         (run-jetty app# {:port port#
                          :join? false})]

     (try
       ~@body
       (finally
         (.stop server#)))))



(defn ->result

  ([result]
   (->result nil result))

  ([id result]
   {:status 200
    :body {:id id
           :jsonrpc "2.0"
           :result result}}))


(defn ->error [error]
  {:status 400
   :body {:id 1
          :jsonrpc "2.0"
          :error error}})


(comment

  (def _h
    (make-handler
     {:method :post
      :path "/api"
      :data-field :params
      :methods
      {:user/get-by-id
       {:status 200
        :body {:result {:name "Ivan"
                        :email "test@test.com"}}}}}))

  (_h {:request-method :post :uri "/api"})


  (with-stub
    {:methods
     {:user/get-by-id
      {:status 200
       :body {:result {:name "Ivan"
                       :email "test@test.com"}}}}}

    (println 1)))
