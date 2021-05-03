(ns farseer.config)


(defn query-keys [config ns]
  (persistent!
   (reduce-kv
    (fn [result k v]
      (if (= (namespace k) (name ns))
        (assoc! result (keyword (name k)) v)
        result))
    (transient {})
    config)))


(defn rebase [config defaults]
  (merge defaults config))
