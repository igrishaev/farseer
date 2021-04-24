(ns farseer.jetty-test
  (:require
   [farseer.server.jetty :as jetty]
   [farseer.server.http :as http]

   [com.stuartsierra.component :as component]
   [clj-http.client :as client]
   [ring.middleware.basic-authentication
    :refer [wrap-basic-authentication]]

   [clojure.test :refer [deftest is]]))


(def PORT 18008)

(def PATH "/v1/api-rpc")

(def config
  {:jetty/port PORT
   :http/path PATH
   :rpc/handlers
   {:test/add
    {:handler/function
     (fn [_ [a b]]
       (+ a b))}}})


(def url-server
  (format "http://127.0.0.1:%s%s" PORT PATH))


(def url-health
  (format "http://127.0.0.1:%s%s" PORT "/health"))


(deftest test-jetty-ok

  (jetty/with-server [config]

    (let [resp (client/post
                url-server
                {:as :json
                 :content-type :json
                 :throw-exceptions? false
                 :coerce :always
                 :form-params {:id 1
                               :jsonrpc "2.0"
                               :method :test/add
                               :params [1 2]}})

          {:keys [status body]} resp]

      (is (= 200 status))
      (is (= {:id 1 :jsonrpc "2.0" :result 3}
             body)))))


(deftest test-jetty-health

  (jetty/with-server [config]

    (let [resp (client/get url-health)

          {:keys [status body]} resp]

      (is (= 200 status))
      (is (= "" body)))))


(defn make-system [config]
  (component/system-map
   :db {:jdbc :spec}
   :rpc-server (component/using
                (jetty/component config {:some :param})
                {:database :db})))


(deftest test-jetty-component

  (let [sys-init (make-system config)
        sys-started (component/start sys-init)]

    (let [resp (client/get url-health)]

      (is (some? (-> sys-started :rpc-server :jetty/server)))
      (is (= 200 (:status resp))))

    (let [sys-stopped (component/stop sys-started)]

      (is (nil? (-> sys-stopped :rpc-server :jetty/server)))

      (is (thrown?
           java.net.ConnectException
           (client/get url-health))))))


(deftest test-jetty-component-context

  (let [capture (atom nil)

        config*
        (assoc-in config
                  [:rpc/handlers :test/capture :handler/function]
                  (fn [context params]
                    (reset! capture context)
                    {:result 1}))

        sys-init (make-system config*)
        sys-started (component/start sys-init)]

    (let [resp (client/post
                url-server
                {:as :json
                 :content-type :json
                 :throw-exceptions? false
                 :coerce :always
                 :form-params {:id 1
                               :jsonrpc "2.0"
                               :method :test/capture}})

          {:keys [status body]} resp]

      (is (= {:id 1 :jsonrpc "2.0" :result {:result 1}}
             body)))

    (is (=

         {:some :param
          :database {:jdbc :spec}
          :rpc/id 1
          :rpc/method :test/capture
          :http/request
          {:ssl-client-cert nil
           :protocol "HTTP/1.1"
           :remote-addr "127.0.0.1"
           :headers
           {"connection" "close"
            "user-agent" "Apache-HttpClient/4.5.13 (Java/14.0.2)"
            "host" (format "127.0.0.1:%s" PORT)
            "accept-encoding" "gzip, deflate"
            "content-length" "48"
            "content-type" "application/json"}
           :server-port PORT
           :content-length 48
           :content-type "application/json"
           :character-encoding "UTF-8"
           :uri "/v1/api-rpc"
           :server-name "127.0.0.1"
           :query-string nil
           :body {:id 1 :jsonrpc "2.0" :method "test/capture"}
           :scheme :http
           :request-method :post}}

         @capture))

    (component/stop sys-started)))


(deftest test-auth-middleware

  (let [fn-auth?
        (fn [user pass]
          (and (= "foo" user)
               (= "bar" pass)))

        middleware-auth
        [wrap-basic-authentication
         fn-auth? "auth" http/non-auth-response]

        middleware-stack
        [middleware-auth
         http/wrap-json-body
         http/wrap-json-resp]

        config*
        (assoc config :http/middleware middleware-stack)]

    (jetty/with-server [config*]

      (let [resp1 (client/post
                   url-server
                   {:basic-auth ["foo" "bar"]
                    :as :json
                    :content-type :json
                    :throw-exceptions? false
                    :coerce :always
                    :form-params {:id 1
                                  :jsonrpc "2.0"
                                  :method :test/add
                                  :params [1 2]}})

            {:keys [status body]} resp1]

        (is (= 200 status))
        (is (= {:id 1 :jsonrpc "2.0" :result 3}
               body)))

      (let [resp2 (client/post
                   url-server
                   {:basic-auth ["test" "dunno"]
                    :as :json
                    :content-type :json
                    :throw-exceptions? false
                    :coerce :always
                    :form-params {:id 1
                                  :jsonrpc "2.0"
                                  :method :test/add
                                  :params [1 2]}})

            {:keys [status body]} resp2]

        (is (= 200 status))
        (is (= {:jsonrpc "2.0"
                :error {:code -32000
                        :message "Authentication failure."}}
             body))))))
