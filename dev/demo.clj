(ns demo
  (:require
   [farseer.handler :refer [make-handler]]
   [clojure.spec.alpha :as s]))


(s/def :math/sum.in
  (s/tuple number? number?))


(s/def :math/sum.out
  number?)


(defn rpc-sum
  [_ [a b]]
  (+ a b))


#_
(def config
  {:rpc/handlers
   {:math/sum
    {:handler/function #'rpc-sum}}})




(def config
  {:rpc/handlers
   {:math/sum
    {:handler/function #'rpc-sum
     :handler/spec-in :math/sum.in
     :handler/spec-out :math/sum.out}}})

(def handler
  (make-handler config))


#_
(handler {:id 1
          :method :math/sum
          :params [1 2]
          :jsonrpc "2.0"})

#_
{:id 1, :jsonrpc "2.0", :result 3}


#_
(handler {:id 1
          :method :system/rmrf
          :params [1 2]
          :jsonrpc "2.0"})

#_
{:error
 {:code -32601, :message "Method not found", :data {:method :system/rmrf}},
 :id 1,
 :jsonrpc "2.0"}


#_
(handler {:id 1
          :method :math/sum
          :params ["one" nil]
          :jsonrpc "2.0"})



;;;;;;;;;;;;;;; ---------


(s/def :sum/a number?)
(s/def :sum/b number?)

(s/def :math/sum.in
  (s/keys :req-un [:sum/a :sum/a]))

(s/def :math/sum.out
  number?)


(defn rpc-sum
  [_ {:keys [a b]}]
  (+ a b))


(def config
  {:rpc/handlers
   {:math/sum
    {:handler/function #'rpc-sum
     :handler/spec-in :math/sum.in
     :handler/spec-out :math/sum.out}}})


(def handler
  (make-handler config))

(handler {:id 1
          :method :math/sum
          :params {:a 1 :b 2}
          :jsonrpc "2.0"})



(defn rpc-sum
  [context {:keys [a b]}]
  (println context)
  (+ a b))


(s/def :math/sum.out
  string?)



#_
(defn get-user-by-id
  [{:keys [db]}
   {:keys [user-id]}]
  (jdbc/get-by-id db :users user-id))


(s/def :user/id pos-int?)

(s/def :user/user-by-id.in
  (s/keys :req-un [:user/id]))

(s/def :user/user-by-id.out
  (s/nilable map?))


#_
(def config
  {:rpc/handlers
   {:user/get-by-id
    {:handler/function #'get-user-by-id
     :handler/spec-in :user/user-by-id.in
     :handler/spec-out :user/user-by-id.out}}})

#_
(def handler
  (make-handler config))

#_
(handler {:id 1
          :method :user/get-by-id
          :params {:id 5}
          :jsonrpc "2.0"}
         {:db hikari-cp-pool})


#_
(def handler
  (make-handler
   config
   {:db (open-db-connection {... ...})}))


(defn rpc-sum
  [context [a b]]
  (println context)
  (+ a b))

(def config
  {:rpc/handlers
   {:math/sum
    {:handler/function #'rpc-sum}}})

(def handler
  (make-handler config {:foo 1}))


#_
(handler {:id 1
          :method :math/sum
          :params [1 2]
          :jsonrpc "2.0"}
         {:foo 2}
         )



;;;;;;; --------------


(defn rpc-div
  [_ [a b]]
  (/ a b))

(def config
  {:rpc/handlers
   {:math/div
    {:handler/function #'rpc-div}}})

(def handler
  (make-handler config))


(handler {:id 1
          :method :math/div
          :params [1 0]
          :jsonrpc "2.0"})

{:error {:code -32603, :message "Internal error", :data {:method :math/div}}, :id 1, :jsonrpc "2.0"}
