(ns farseer.client-test
  (:require
   [farseer.client :as client]
   [farseer.server.jetty :as jetty]

   [clojure.test :refer [deftest is use-fixtures]]))


(def PORT 8808)


(def config-client
  {:http/url (format "http://127.0.0.1:%s" PORT)})


(def config-server
  {:jetty/port PORT
   :rpc/handlers
   {:user/get-by-id
    {:handler/function
     (fn [& _]
       {:name "Ivan"})}}})


(defn jetty-fixture [t]
  (let [server (jetty/start-server config-server)]
    (t)
    (jetty/stop-server server)))


(use-fixtures :once jetty-fixture)


(deftest test-client-ok

  (let [client (client/make-client config-client)
        result (client/call client :user/get-by-id [1])]

    (is (= 1 result))





    )

)



#_
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
