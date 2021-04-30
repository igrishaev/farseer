(ns farseer.http-test
  (:require
   [farseer.http :as http]

   [cheshire.core :as json]

   [clojure.spec.alpha :as s]
   [clojure.test :refer [deftest is]]))



(def config
  {:http/method :post
   :rpc/handlers
   {:test/sum
    {:handler/function (fn [_ params]
                         (reduce + 0 params))}}})


;; add ring-mock

(deftest test-http-app-ok

  (let [app (http/make-app config)

        req {:request-method :post
             :uri "/"
             :body {:id 1
                    :jsonrpc "2.0"
                    :method :test/sum
                    :params [1 2 3]}}

        res (-> (app req)
                (update :body json/parse-string true))]

    (is (= {:status 200,
            :body {:id 1 :jsonrpc "2.0" :result 6}
            :headers {"Content-Type" "application/json; charset=utf-8"}}

           res))))
