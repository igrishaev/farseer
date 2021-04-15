(ns farseer.jetty-test
  (:require
   [farseer.server.jetty :as jetty]

   [clj-http.client :as client]

   [clojure.test :refer [deftest is]]))


(def config
  {:handlers
   {:test/add
    {:handler
     (fn [_ a b]
       (+ a b))}}})


(deftest test-jetty-ok

  (let [server (jetty/start-server config)

        resp (client/post "http://127.0.0.1:8080"
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
           body))

    (.stop server)
    )

  )
