(ns farseer.server.http
  (:require
   [farseer.config :as config]
   [farseer.error :as e]
   [farseer.handler :as handler]

   [ring.middleware.json
    :refer [wrap-json-body
            wrap-json-response]]))


(def malformed-response
  {:status 400
   :headers {"Content-Type" "application/json"}
   :body
   {:jsonrpc "2.0"
    :error
    {:code -32700
     :message
     "Invalid JSON was received by the server."}}})


(def json-body-options
  {:keywords? true
   :malformed-response malformed-response})


(def defaults
  {:http/method :post
   :http/path "/"
   :http/health? true})


(defn make-app

  ([config]
   (make-app config nil))

  ([config context]
   (let [config
         (config/add-defaults config defaults)

         handler
         (handler/make-handler config context)

         {:http/keys [method path health?]}
         config]

     (->

      (fn [{:as request
            :keys [uri
                   body
                   request-method]}]

        (cond

          (and (= method request-method)
               (= path uri))
          (let [response
                (handler body {:request request})]
            {:status 200
             :body response})

          (and health?
               (or (= "/health" uri)
                   (= "/healthz" uri)))
          {:status 200}

          :else
          {:status 404}))

      (wrap-json-body json-body-options)
      (wrap-json-response)))))
