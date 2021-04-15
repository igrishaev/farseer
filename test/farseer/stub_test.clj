(ns ^:integration
    farseer.stub-test
  (:require
   [farseer.stub :as stub]

   [clj-http.client :as client]

   [clojure.test :refer [deftest is]]))


(def config
  {:stub/methods

   {:user/get-by-id
    {:name "Ivan"
     :email "test@test.com"}

    ;; :some/invalid-params
    ;; (stub/->rpc-error :invalid-params)

    :some/failure
    (fn [& _]
      (/ 0 0))}})


(deftest test-stub-ok

  (let [params
        {:method :post
         :url "http://127.0.0.1:8080"
         :throw-exceptions? false
         :as :json
         :content-type :json
         :coerce :always
         :form-params
         {:id 1
          :jsonrpc "2.0"
          :method :user/get-by-id
          :params [100]}}]

    (stub/with-stub config

      (let [response
            (-> params
                client/request
                (select-keys [:status :body]))]

        (is (= {:status 200
                :body
                {:id 1
                 :jsonrpc "2.0"
                 :result {:name "Ivan"
                          :email "test@test.com"}}}
             response))))))


(deftest test-stub-not-found

  (let [params
        {:method :post
         :url "http://127.0.0.1:8008/api"
         :throw-exceptions? false
         :as :json
         :content-type :json
         :coerce :always
         :form-params
         {:id 1
          :jsonrpc "2.0"
          :method :dunno/not-found
          :params [100]}}]

    (stub/with-stub config

      (let [response
            (-> params
                client/request
                (select-keys [:status :body]))]

        (is (= {:status 404
                :body
                {:id 1
                 :jsonrpc "2.0"
                 :error
                 {:code -32601
                  :message "Method not found"
                  :data {:method "dunno/not-found"}}}}
             response))))))


#_
(deftest test-stub-failing

  (let [params
        {:method :post
         :url "http://127.0.0.1:8008/api"
         :throw-exceptions? false
         :as :json
         :content-type :json
         :coerce :always
         :form-params
         {:id 1
          :jsonrpc "2.0"
          :method :some/failure
          :params [100]}}]

    (stub/with-stub config

      (let [response
            (-> params
                client/request
                (select-keys [:status :body]))]

        (is (=

             {:status 500
              :body
              {:id 1
               :jsonrpc "2.0"
               :error
               {:code -32603
                :message "Internal error"
                :data {:method "some/failure"}}}}

             response))))))


(deftest test-stub-custom-invalid-params

  (let [params
        {:method :post
         :url "http://127.0.0.1:8008/api"
         :throw-exceptions? false
         :as :json
         :content-type :json
         :coerce :always
         :form-params
         {:id 1
          :jsonrpc "2.0"
          :method :some/invalid-params
          :params [100]}}]

    (stub/with-stub config

      (let [response
            (-> params
                client/request
                (select-keys [:status :body]))]

        (is (=

             {:status 400
              :body
              {:id 1
               :jsonrpc "2.0"
               :error
               {:code -32602
                :message "Invalid params"
                :data {:method "some/invalid-params"}}}}

             response))))))


(deftest test-stub-malformed-json

  (let [params
        {:method :post
         :url "http://127.0.0.1:8008/api"
         :throw-exceptions? false
         :as :json
         :coerce :always
         :content-type :json
         :body "not a JSON"}]

    (stub/with-stub config

      (let [response
            (-> params
                client/request
                (select-keys [:status :body]))]

        (is (=

             {:status 400
              :body
              {:jsonrpc "2.0"
               :error
               {:code -32700
                :message "Invalid JSON was received by the server."}}}

             response))))))
