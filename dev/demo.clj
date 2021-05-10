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


(s/def :math/sum.out
  string?)
