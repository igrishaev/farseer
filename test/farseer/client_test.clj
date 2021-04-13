(ns farseer.client-test
  (:require
   [farseer.client :as client]
   [farseer.stub :as stub]

   [clojure.test :refer [deftest is]]))


(def config
  {:method-options
   {:some/method
    {:rpc/notify? true}}

   :handlers

   {:user/get-by-id
    {:name "Ivan"
     :email "test@test.com"}

    :some/invalid-params
    (stub/->rpc-error :invalid-params)

    :some/failure
    (fn [& _]
      (/ 0 0))}})


(deftest test-client-ok

  (stub/with-stub config

    (let [cfg (client/make-config nil)

          resp
          (client/call cfg :user/get-by-id 42)]

      (is (= {:name "Ivan" :email "test@test.com"}
             resp)))))


(deftest test-client-notify-ok

  (stub/with-stub config

    (let [cfg (client/make-config nil)

          resp
          (client/notify cfg :user/get-by-id 42)]

      (is (nil? resp)))))



(deftest test-client-batch-ok

  (stub/with-stub config

    (let [cfg (client/make-config nil)

          resp
          (client/batch cfg
                        [[:user/get-by-id [1]]
                         [:user/get-by-id [2]]
                         [:user/get-by-id [3]]])]

      (is (=

           [{:id 42
             :jsonrpc "2.0"
             :result {:name "Ivan" :email "test@test.com"}}
            {:id 42
             :jsonrpc "2.0"
             :result {:name "Ivan" :email "test@test.com"}}
            {:id 42
             :jsonrpc "2.0"
             :result {:name "Ivan" :email "test@test.com"}}]

           resp)
       ))))
