(ns farseer.server.http
  (:require
   [farseer.config :as config]
   [farseer.error :as e]

   [farseer.spec.http :as spec.http]
   [farseer.handler :as handler]

   [ring.middleware.json :as json]

   [clojure.spec.alpha :as s]))


(def malformed-response
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body
   {:jsonrpc "2.0"
    :error
    {:code -32700
     :message "Invalid JSON was received by the server."}}})


(def non-auth-response
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body
   {:jsonrpc "2.0"
    :error
    {:code -32000
     :message "Authentication failure."}}})


(def json-body-options
  {:keywords? true
   :malformed-response malformed-response})


(def wrap-json-body
  [json/wrap-json-body json-body-options])

(def wrap-json-resp
  [json/wrap-json-response])


(def default-middleware
  [wrap-json-body
   wrap-json-resp])


(def defaults
  {:http/method :post
   :http/path "/"
   :http/health? true
   :http/middleware default-middleware})


(defn wrap-middleware
  [handler middleware-list]
  (reduce
   (fn [result middleware]
     (if (vector? middleware)

       (let [[middleware & args] middleware]
         (apply middleware result args))

       (middleware result)))

   handler
   middleware-list))


(defn make-app

  ([config]
   (make-app config nil))

  ([config context]

   (let [config
         (config/add-defaults config defaults)]

     (s/assert ::spec.http/config config)

     (let [handler
           (handler/make-handler config context)

           {:http/keys [path
                        method
                        health?
                        middleware]} config]

       (->

        (fn [{:as request
              :keys [uri
                     body
                     request-method]}]

          (cond

            (and (= method request-method)
                 (= path uri))
            (let [response
                  (handler body {:http/request request})]
              {:status 200
               :body response})

            (and health?
                 (or (= "/health" uri)
                     (= "/healthz" uri)))
            {:status 200}

            :else
            {:status 404}))

        (wrap-middleware middleware))))))
