(ns farseer.client-test
  (:require
   [farseer.client :as client]
   [farseer.server.jetty :as jetty]

   [com.stuartsierra.component :as component]

   [clojure.test :refer [deftest is use-fixtures]])

  (:import
   org.apache.http.conn.HttpClientConnectionManager))


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


(defn drop-id [result]
  (dissoc result :id))


(deftest test-client-ok

  (let [client (client/make-client config-client)
        result (client/call client :user/get-by-id [1])]

    (is (int? (:id result)))

    (is (= {:jsonrpc "2.0" :result {:name "Ivan"}}
           (drop-id result)))))



(deftest test-client-notify

  (let [client (client/make-client config-client)
        result (client/notify client :user/get-by-id [1])]

    (is (nil? result))))


(deftest test-client-batch-ok

  (let [client (client/make-client config-client)
        result (client/batch client
                             [[:user/get-by-id [1]]
                              [:user/get-by-id [2]]
                              [:user/get-by-id [3]]])]

    (is (=

         [{:jsonrpc "2.0" :result {:name "Ivan"}}
          {:jsonrpc "2.0" :result {:name "Ivan"}}
          {:jsonrpc "2.0" :result {:name "Ivan"}}]

         (mapv drop-id result)))))


(deftest test-client-batch-notify

  (let [client (client/make-client config-client)
        result (client/batch client
                             [[:user/get-by-id [1]]
                              ^:rpc/notify
                              [:user/get-by-id [2]]
                              [:user/get-by-id [3]]])]

    (is (=

         [{:jsonrpc "2.0" :result {:name "Ivan"}}
          {:jsonrpc "2.0" :result {:name "Ivan"}}]

         (mapv drop-id result)))))


(deftest test-conn-manager

  (let [client (client/make-client config-client)]

    (is (nil? (:http/connection-manager client)))

    (client/with-conn-mgr [client* client]

      (instance? HttpClientConnectionManager
                 (:http/connection-manager client*))

      (let [result (client/call client* :user/get-by-id [1])]

        (is (= {:jsonrpc "2.0" :result {:name "Ivan"}}
               (drop-id result)))))))


(defn component? [x]
  (satisfies? component/Lifecycle x))


(deftest test-client-component

  (let [comp (client/component config-client)
        comp-started (component/start comp)]

    (is (component? comp))
    (is (component? comp-started))

    (is (nil? (:http/connection-manager comp)))
    (is (some? (:http/connection-manager comp-started)))

    (let [result (client/call comp-started :user/get-by-id [1])]

      (is (= {:jsonrpc "2.0" :result {:name "Ivan"}}
             (drop-id result))))

    (let [comp-stopped (component/stop comp-started)]

      (is (component? comp-stopped))
      (is (nil? (:http/connection-manager comp-stopped))))))


(deftest test-wrong-config

  (is (thrown-with-msg?
       clojure.lang.ExceptionInfo
       #"Spec assertion failed"

       (client/make-client {}))))


(def wrong-payload-cases
  [[:user/get-by-id 123]
   [:user/get-by-id "test"]])


(deftest test-wrong-payload

  (let [client (client/make-client config-client)]

    (doseq [[method params] wrong-payload-cases]

      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           #"Spec assertion failed"

           (client/call client method params))))))


(def valid-payload-cases
  [[:user/get-by-id nil]
   [:user/get-by-id [1 2 3]]
   [:user/get-by-id {:id 1}]
   [:user/get-by-id {}]])


(deftest test-valid-payload

  (let [client (client/make-client config-client)]

    (doseq [[method params] valid-payload-cases]

      (let [result
            (client/call client method params)]

        (is (= {:jsonrpc "2.0" :result {:name "Ivan"}}
               (drop-id result)))))))
