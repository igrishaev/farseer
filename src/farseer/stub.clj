(ns farseer.stub
  (:require
   [farseer.server.jetty :as jetty]

   [clojure.tools.logging :as log]))


(defn udpate-handlers
  [{:as config
    :stub/keys [methods]}]

  (reduce-kv
   (fn [config method response]
     (assoc-in config
               [:handlers method :handler]
               (if (fn? response)
                 response
                 (fn [& _]
                   response))))
   config
   methods))



(defmacro with-stub
  [config & body]

  `(let [config# (-> ~config
                     udpate-handlers)

         server# (jetty/start-server config#)]

     (try
       ~@body
       (finally
         (.stop server#)))))
