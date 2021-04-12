(ns farseer.client-test
  (:require
   [farseer.client :as client]
   [farseer.stub :as stub]

   [clojure.test :refer [deftest is]]))


(def config
  {:handlers

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
          (client/call cfg :user/get-by-id 42)

          ;; resp (select-keys resp [:status :body])
          ]

      (is (= {:name "Ivan", :email "test@test.com"}
             resp)))))
