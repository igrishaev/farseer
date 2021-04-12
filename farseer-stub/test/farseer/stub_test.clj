(ns ^:integration
    farseer.stub-test
  (:require
   [farseer.stub :as stub]

   [clj-http.client :as client]

   [clojure.test :refer [deftest is]]))


(def config
  {:handlers

   {:user/get-by-id
    {:name "Ivan"
     :email "test@test.com"}

    :some/failure
    (fn [& _]
      (/ 0 0))}})


(deftest test-stub-ok

  (let [params
        {:method :post
         :url "http://127.0.0.1:8008/api"
         :throw-exceptions? false
         :as :json
         :content-type :json
         :coerce :always
         :form-params
         {:id 1
          :version "2.0"
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
          :version "2.0"
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
          :version "2.0"
          :method :some/failure
          :params [100]}}]

    (stub/with-stub config

      (let [response
            (-> params
                client/request
                (select-keys [:status :body]))]

        (is (= {:status 500
                :body
                {:id 1
                 :jsonrpc "2.0"
                 :error {:code nil
                         :message nil
                         :data {:method "some/failure"}}}}
               response))))))
