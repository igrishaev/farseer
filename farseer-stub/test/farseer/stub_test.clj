(ns ^:integration
    farseer.stub-test
  (:require
   [farseer.stub :as stub]

   [clj-http.client :as client]

   [clojure.test :refer [deftest is]]))


(def PORT 18008)


(def config
  {:jetty/port PORT
   :stub/handlers

   {:user/get-by-id
    {:name "Ivan"
     :email "test@test.com"}

    :some/trigger-error
    stub/invalid-request

    :some/failure
    (fn [& _]
      (/ 0 0))}})


(def http-params
  {:method :post
   :url (format "http://127.0.0.1:%s" PORT)
   :throw-exceptions? false
   :as :json
   :content-type :json
   :coerce :always
   :form-params
   {:id 1
    :jsonrpc "2.0"
    :method :user/get-by-id
    :params [100]}})


(deftest test-stub-ok

  (stub/with-stub config

    (let [response
          (-> http-params
              client/request
              (select-keys [:status :body]))]

      (is (= {:status 200
              :body
              {:id 1
               :jsonrpc "2.0"
               :result {:name "Ivan"
                        :email "test@test.com"}}}
             response)))))


(deftest test-stub-not-found

  (let [params
        (assoc-in http-params
                  [:form-params :method]
                  :dunno/not-found)]

    (stub/with-stub config

      (let [response
            (-> params
                client/request
                (select-keys [:status :body]))]

        (is (=

             {:status 200
              :body
              {:error
               {:code -32601
                :message "Method not found"
                :data {:method "dunno/not-found"}}
               :id 1
               :jsonrpc "2.0"}}

             response))))))


(deftest test-stub-failing

  (let [params
        (assoc-in http-params
                  [:form-params :method]
                  :some/failure)]

    (stub/with-stub config

      (let [response
            (-> params
                client/request
                (select-keys [:status :body]))]

        (is (=

             {:status 200
              :body
              {:id 1
               :jsonrpc "2.0"
               :error
               {:code -32603
                :message "Internal error"
                :data {:method "some/failure"}}}}

             response))))))


(deftest test-stub-trigger-error

  (let [params
        (assoc-in http-params
                  [:form-params :method]
                  :some/trigger-error)]

    (stub/with-stub config

      (let [response
            (-> params
                client/request
                (select-keys [:status :body]))]

        (is (=

             {:status 200
              :body
              {:error
               {:code -32600
                :message "Invalid Request"
                :data {:method "some/trigger-error"}}
               :id 1
               :jsonrpc "2.0"}}

             response))))))


(deftest test-stub-malformed-json

  (let [params
        (-> http-params
            (dissoc :form-params)
            (assoc :body "not a json"))]

    (stub/with-stub config

      (let [response
            (-> params
                client/request
                (select-keys [:status :body]))]

        (is (=

             {:status 200
              :body
              {:jsonrpc "2.0"
               :error
               {:code -32700
                :message "Invalid JSON was received by the server."}}}

             response))))))
