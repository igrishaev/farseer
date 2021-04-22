(ns farseer.error)


(def parse-error
  {:rpc/code    -32700
   :rpc/message "Parse error"})


(def invalid-request
  {:rpc/code    -32600
   :rpc/message "Invalid Request"})


(def not-found
  {:rpc/code    -32601
   :rpc/message "Method not found"})


(def invalid-params
  {:rpc/code    -32602
   :rpc/message "Invalid params"})


(def internal-error
  {:rpc/code    -32603
   :rpc/message "Internal error"})


(def auth-error
  {:rpc/code    -32000
   :rpc/message "Authentication failure"})


(defn error!

  ([data]
   (error! data nil))

  ([data cause]
   (throw (ex-info "RPC error" data cause))))


(defn parse-error!
  [& [data e]]
  (error! (merge parse-error data) e))


(defn invalid-request!
  [& [data e]]
  (error! (merge invalid-request data) e))


(defn not-found!
  [& [data e]]
  (error! (merge not-found data) e))


(defn invalid-params!
  [& [data e]]
  (error! (merge invalid-params data) e))


(defn internal-error!
  [& [data e]]
  (error! (merge internal-error data) e))
