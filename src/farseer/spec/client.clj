(ns farseer.spec.client
  (:require
   [clojure.spec.alpha :as s]))


(s/def :rpc/fn-id
  (s/or :id/int  #{:id/int}
        :id/uuid #{:id/uuid}
        :id/fn   fn?))


(s/def :rpc/fn-before-send fn?)

(s/def :http/url    string?)
(s/def :http/method #{:get :post :put :delete})


(s/def ::config
  (s/keys :req [:rpc/fn-id
                :rpc/fn-before-send

                :http/url
                :http/method
                :http/as
                :http/content-type
                :http/throw-exceptions?
                :http/coerce

                :conn-mgr/timeout
                :conn-mgr/threads
                :conn-mgr/default-per-route
                :conn-mgr/insecure?]))
