(ns farseer.http-test
  (:require
   [farseer.http :as http]

   [ring.mock.request :as mock]
   [cheshire.core :as json]

   [clojure.spec.alpha :as s]
   [clojure.test :refer [deftest is]]))



(def config
  {:http/method :post
   :rpc/handlers
   {:test/error
    {:handler/function
     (fn [_ params]
       (/ 0 0))}
    :test/sum
    {:handler/function
     (fn [_ params]
       (reduce + 0 params))}}})


(defn parse-body [response]
  (update response :body json/parse-string true))


(deftest test-http-app-ok

  (let [app (http/make-app config)

        body {:id 1
              :jsonrpc "2.0"
              :method :test/sum
              :params [1 2 3]}

        req (-> (mock/request :post "/")
                (mock/json-body body))

        res (-> req
                app
                parse-body)]

    (is (= {:status 200,
            :body {:id 1 :jsonrpc "2.0" :result 6}
            :headers {"Content-Type" "application/json; charset=utf-8"}}

           res))))


(deftest test-http-app-404

  (let [app (http/make-app config)

        body {:id 1
              :jsonrpc "2.0"
              :method :test/foobar
              :params [1 2 3]}

        req (-> (mock/request :post "/")
                (mock/json-body body))

        res (-> req
                app
                parse-body)]

    (is (= {:status 200,
            :body {:error
                   {:code -32601
                    :message "Method not found"
                    :data {:method "test/foobar"}}
                   :id 1
                   :jsonrpc "2.0"}
            :headers {"Content-Type" "application/json; charset=utf-8"}}

           res))))


(deftest test-http-app-exception

  (let [app (http/make-app config)

        body {:id 1
              :jsonrpc "2.0"
              :method :test/error
              :params [1 2 3]}

        req (-> (mock/request :post "/")
                (mock/json-body body))

        res (-> req
                app
                parse-body)]

    (is (= {:status 200,
            :body {:error
                   {:code -32603
                    :message "Internal error"
                    :data {:method "test/error"}}
                   :id 1
                   :jsonrpc "2.0"}
            :headers {"Content-Type" "application/json; charset=utf-8"}}

           res))))
