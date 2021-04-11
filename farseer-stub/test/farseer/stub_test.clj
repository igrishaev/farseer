(ns farseer.stub-test
  (:require
   [farseer.stub :as stub]

   [clj-http.client :as client]

   [clojure.test :refer [deftest is]]))


(def config
  {:handlers
   {:user/get-by-id
    {:name "Ivan"
     :email "test@test.com"}}})


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
