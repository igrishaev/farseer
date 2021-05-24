(ns demo
  (:require
   [farseer.http :as http]
   [farseer.jetty :as jetty]
   [farseer.handler :refer [make-handler]]
   [com.stuartsierra.component :as component]

   [cheshire.core :as json]

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


#_
(handler [{:id 1
           :method :math/sum
           :params [1 2]
           :jsonrpc "2.0"}
          {:id 2
           :method :math/sum
           :params [3 4]
           :jsonrpc "2.0"}
          {:id 3
           :method :math/sum
           :params [5 6]
           :jsonrpc "2.0"}

          ])


(def config
  {:rpc/batch-allowed? false
   :rpc/handlers
   {:math/sum
    {:handler/function #'rpc-sum
     :handler/spec-in :math/sum.in
     :handler/spec-out :math/sum.out}}})

(def handler
  (make-handler config))


(handler [{:id 1
           :method :math/sum
           :params [1 2]
           :jsonrpc "2.0"}
          {:id 2
           :method :math/sum
           :params [3 4]
           :jsonrpc "2.0"}])

{:error {:code -32602, :message "Batch is not allowed"}}


(def config
  {:rpc/batch-allowed? true
   :rpc/batch-max-size 2
   :rpc/handlers
   {:math/sum
    {:handler/function #'rpc-sum
     :handler/spec-in :math/sum.in
     :handler/spec-out :math/sum.out}}})

(def handler
  (make-handler config))


(handler [{:id 1
           :method :math/sum
           :params [1 2]
           :jsonrpc "2.0"}
          {:id 2
           :method :math/sum
           :params [3 4]
           :jsonrpc "2.0"}
          {:id 3
          :method :math/sum
          :params [5 6]
          :jsonrpc "2.0"}]

         )

{:error {:code -32602, :message "Batch size is too large"}}


(def config
  {:rpc/handlers
   {:math/sum
    {:handler/function #'rpc-sum
     :handler/spec-in :math/sum.in
     :handler/spec-out :math/sum.out}}})

(def app
  (http/make-app config))

(def rpc
  {:id 1
   :jsonrpc "2.0"
   :method :math/sum
   :params [1 2]})

(def request
  {:request-method :post
   :uri "/"
   :headers {"content-type" "application/json"}
   :body (-> rpc json/generate-string .getBytes)})

(def response
  (-> (app request)
      (update :body json/parse-string true)))



(def rpc
  {:id 1
   :jsonrpc "2.0"
   :method :math/missing
   :params [nil "a"]})

(def request
  {:request-method :post
   :uri "/"
   :headers {"content-type" "application/json"}
   :body (-> rpc json/generate-string .getBytes)})

(def response
  (-> (app request)
      (update :body json/parse-string true)))

{:status 200,
 :body
 {:error
  {:code -32601, :message "Method not found", :data {:method "math/missing"}},
  :id 1,
  :jsonrpc "2.0"},
 :headers {"Content-Type" "application/json; charset=utf-8"}}



(def rpc
  [{:id 1
    :jsonrpc "2.0"
    :method :math/sum
    :params [1 2]}
   {:id 2
    :jsonrpc "2.0"
    :method :math/sum
    :params [3 4]}])

(def request
  {:request-method :post
   :uri "/"
   :headers {"content-type" "application/json"}
   :body (-> rpc json/generate-string .getBytes)})

(def response
  (-> (app request)
      (update :body json/parse-string true)))


#_
{:status 200,
 :body ({:id 1, :jsonrpc "2.0", :result 3} {:id 2, :jsonrpc "2.0", :result 7}),
 :headers {"Content-Type" "application/json; charset=utf-8"}}


;; auth

(defn auth? [user pass]
  (and (= "foo" user)
       (= "bar" pass)))

(def middleware-stack
  [[wrap-basic-authentication auth? "Please auth" http/non-auth-response]
   [http/wrap-json-body http/json-body-options]
   http/wrap-json-resp])


(def config
  {:http/middleware middleware-stack
   :rpc/handlers
   {:math/sum
    {:handler/function #'rpc-sum
     :handler/spec-in :math/sum.in
     :handler/spec-out :math/sum.out}}})


(def config
  {:rpc/handlers
   {:math/sum
    {:handler/function (fn [ctx params]
                         (println ctx))}}})


(defn some-rpc [context params]
  (let [{:http/keys [request]} context
        {:keys [user]} request]
    (when-not request
      (throw ...))))

(def app
  (http/make-app config))

(def rpc
  {:id 1
   :jsonrpc "2.0"
   :method :math/sum
   :params [1 2]})

(def request
  {:request-method :post
   :uri "/"
   :headers {"content-type" "application/json"}
   :body (-> rpc json/generate-string .getBytes)})

(def response
  (-> (app request)
      (update :body json/parse-string true)))

#_
{:http/request {:request-method :post, :uri /, :headers {content-type application/json}, :body {:id 1, :jsonrpc 2.0, :method math/sum, :params [1 2]}}, :rpc/id 1, :rpc/method :math/sum}


(def app
  (make-app config {:app/version "0.0.1"}))

(defn rpc-func [context params]
  {:message (str "The version is " (:app/version context))})


(def config
  {:rpc/handlers
   {:math/sum
    {:handler/function #'rpc-sum
     :handler/spec-in :math/sum.in
     :handler/spec-out :math/sum.out}}})

(def server
  (jetty/start-server config))

(jetty/stop-server server)


(jetty/with-server [config {:foo 42}]
  (println 1 2 3))



#_
(require '[com.stuartsierra.component :as component])

(def jetty
  (jetty/component config {:some "field"}))

(def jetty-started
  (component/start jetty))

(def jetty-stopped
  (component/stop jetty-started))


(defn make-system
  [rpc-config db-config cache-config]
  (component/system-using
   (component/system-map
    :cache (cache-component cache-config)
    :db-pool (db-component db-config)
    :rpc-server (jetty/component rpc-config))
   {:rpc-server [:db-pool :cache]}))


(defn rpc-user-get-by-id
  [{:keys [db-pool cache]} [user-id]]
  (or (get-user-from-cache cache user-id)
      (get-user-from-db db-pool user-id)))
