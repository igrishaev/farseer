(ns farseer.handler-test
  (:require
   [farseer.handler :refer [make-handler]]

   [clojure.spec.alpha :as s]
   [clojure.test :refer [deftest is]]))


(s/def :math/sum.in
  (s/tuple number? number?))


(s/def :math/sum.out
  number?)


(defn rpc-sum
  [_ a b]
  (+ a b))


(defn user-create
  [_ {:keys [name age email]}]
  {:id 100
   :name name
   :age age
   :email email})


(s/def :user/create.in
  (s/keys :req-un [:user/name
                   :user/age
                   :user/email]))


(s/def :user/create.out
  (s/keys :req-un [:user/id
                   :user/name
                   :user/age
                   :user/email]))


(def config
  {:handlers
   {:math/sum
    {:handler #'rpc-sum
     :spec-in :math/sum.in
     :spec-out :math/sum.out}

    :user/create
    {:handler user-create
     :spec-in :user/create.in
     :spec-out :user/create.out}}})


(deftest test-handler-ok

  (let [rpc {:id 1
             :method :math/sum
             :params [1 2]
             :jsonrpc "2.0"}
        request {:body rpc}
        handler (make-handler config)

        response (handler request)]

    (is (= {:status 200
            :body {:id 1 :jsonrpc "2.0" :result 3}}
           response))))


(deftest test-handler-map-params

  (let [rpc {:id 1
             :method "user/create"
             :params {:name "Ivan"
                      :age 35
                      :email "test@testc.com"}
             :jsonrpc "2.0"}
        request {:body rpc}
        handler (make-handler config)

        response (handler request)]

    (is (= {:status 200
            :body
            {:id 1
             :jsonrpc "2.0"
             :result {:id 100
                      :name "Ivan"
                      :age 35
                      :email "test@testc.com"}}}
           response))))


(s/def :user/create.out-wrong
  (s/keys :req-un [:extra/field
                   :user/id]))


(deftest test-handler-wrong-out-spec

  (let [rpc {:id 1
             :method "user/create"
             :params {:name "Ivan"
                      :age 35
                      :email "test@testc.com"}
             :jsonrpc "2.0"}
        request {:body rpc}

        config*
        (assoc-in config
                  [:handlers :user/create :spec-out]
                  :user/create.out-wrong)

        handler (make-handler config*)

        response (handler request)]

    (is (=

         {:status 500
          :body {:id 1
                 :jsonrpc "2.0"
                 :error {:code -32603
                         :message "Internal error"
                         :data {:method "user/create"}}}}

           response))))


(deftest test-handler-notify-ok

  (let [rpc {:method "math/sum"
             :params [1 2]
             :jsonrpc "2.0"}
        request {:body rpc}
        handler (make-handler config)

        response (handler request)]

    (is (= {:status 200 :body nil}
           response))))


(deftest test-handler-wrong-params

  (let [rpc {:id 1
             :method "math/sum"
             :params [1 nil]
             :jsonrpc "2.0"}
        request {:body rpc}
        handler (make-handler config)

        response (handler request)]

    (is (= {:status 400
            :body
            {:id 1
             :jsonrpc "2.0"
             :error {:code -32602
                     :message "Invalid params"
                     :data
                     {:method "math/sum"
                      :explain
                      "nil - failed: number? in: [1] at: [1] spec: :math/sum.in\n"}}}}

           response))))


(deftest test-handler-batch-ok

  (let [rpc [{:id 1
              :method "math/sum"
              :params [1 2]
              :jsonrpc "2.0"}
             {:id 2
              :method "math/sum"
              :params [3 4]
              :jsonrpc "2.0"}]
        request {:body rpc}
        handler (make-handler config)

        response (handler request)]

    (is (= {:status 200
            :body
            [{:id 1 :jsonrpc "2.0" :result 3}
             {:id 2 :jsonrpc "2.0" :result 7}]}
           response))))


(deftest test-handler-batch-one-fails

  (let [rpc [{:id 1
              :method "math/sum"
              :params [1 2]
              :jsonrpc "2.0"}
             {:id 2
              :method "math/sum"
              :params [3 nil]
              :jsonrpc "2.0"}
             {:id 3
              :method "math/sum"
              :params [5 6]
              :jsonrpc "2.0"}]

        request {:body rpc}
        handler (make-handler config)

        response (handler request)]

    (is (=

         {:status 200
          :body
          [{:id 1 :jsonrpc "2.0" :result 3}
           {:id 2
            :jsonrpc "2.0"
            :error
            {:code -32602
             :message "Invalid params"
             :data {:method "math/sum"
                    :explain "nil - failed: number? in: [1] at: [1] spec: :math/sum.in\n"}}}
           {:id 3 :jsonrpc "2.0" :result 11}]}

         response))))


(deftest test-handler-custom-context

  (let [rpc {:id 1
             :method "custom/context"
             :params [1 2]
             :jsonrpc "2.0"}

        capture (atom nil)

        config
        (assoc-in config [:handlers :custom/context :handler]
                  (fn [& args]
                    (reset! capture args)
                    {:foo 1}))


        request {:body rpc}
        handler (make-handler config {:this "foo"
                                      :that "bar"})

        response (handler request)

        [context & args] @capture]

    (is (=

         {:this "foo"
          :that "bar"
          :request {:body
                    {:id 1
                     :method "custom/context"
                     :params [1 2]
                     :jsonrpc "2.0"}}}

           context))

    (is (= [1 2] args))

    (is (=

         {:status 200
          :body {:id 1
                 :jsonrpc "2.0"
                 :result {:foo 1}}}

         response))))


(deftest test-handler-wrong-payload

  (let [rpc {:foo 42 :test "aa"}

        request {:body rpc}
        handler (make-handler config)

        response (handler request)]

    (is (=

         {:status 400,
           :body
           {:id nil
            :jsonrpc "2.0"
            :error
            {:code -32600,
             :message "Invalid Request"
             :data
             {:explain
              "[:foo 42] - failed: map? in: [0] at: [:batch] spec: :farseer.spec.handler/rpc-single\n[:test \"aa\"] - failed: map? in: [1] at: [:batch] spec: :farseer.spec.handler/rpc-single\n{:foo 42, :test \"aa\"} - failed: (contains? % :jsonrpc) at: [:single] spec: :farseer.spec.handler/rpc-single\n{:foo 42, :test \"aa\"} - failed: (contains? % :method) at: [:single] spec: :farseer.spec.handler/rpc-single\n",
              :method nil}}}}

         response))))


;; batch exception
;; batch allowed?
;; batch limit
;; batch notification
;; batch parallel
;; method overrides

;; in-validation off
;; out-validation off
