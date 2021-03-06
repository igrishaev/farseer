(ns farseer.stub
  (:require
   [farseer.jetty :as jetty]
   [farseer.spec.stub :as spec.stub]
   [farseer.error :as e]

   [clojure.tools.logging :as log]
   [clojure.spec.alpha :as s]))


(def ex? (partial instance? Throwable))


(defn make-rpc-handlers [stub-handlers]
  (reduce-kv
   (fn [rpc-handlers method result]
     (assoc-in rpc-handlers
               [method :handler/function]
               (cond
                 (fn? result)
                 result

                 (ex? result)
                 (fn [& _]
                   (throw result))

                 :else
                 (fn [& _]
                   result))))
   {}
   stub-handlers))


(defn update-handlers
  [config]
  (let [{stub-handlers :stub/handlers} config
        rpc-handlers (make-rpc-handlers stub-handlers)]
    (assoc config :rpc/handlers rpc-handlers)))


(defmacro with-stub
  [config & body]

  `(let [config# ~config]

     (s/assert ::spec.stub/config config#)

     (let [config# (update-handlers config#)
           server# (jetty/start-server config#)]

       (try
         ~@body
         (finally
           (jetty/stop-server server#))))))


(defn with-stubs-impl
  [[& configs] & body]

  (loop [form  `(do ~@body)
         configs (reverse configs)]

    (let [[config & configs] configs]

      (if config
        (recur `(with-stub ~config ~form)
               configs)
        form))))


(defmacro with-stubs
  [[& configs] & body]
  (apply with-stubs-impl configs body))


;;
;; Handlers that trigger RPC exceptions.
;;

(defn ->err-handler [data-error]
  (fn [context params]
    (e/error! data-error)))


(def parse-error
  (->err-handler e/parse-error))


(def invalid-request
  (->err-handler e/invalid-request))


(def not-found
  (->err-handler e/not-found))


(def invalid-params
  (->err-handler e/invalid-params))


(def internal-error
  (->err-handler e/internal-error))


(def auth-error
  (->err-handler e/auth-error))
