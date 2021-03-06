(ns farseer.doc
  (:require
   [farseer.config :as config]
   [farseer.spec.doc :as spec.doc]

   [spec-tools.json-schema :as js]
   [selmer.parser :as parser]
   [selmer.filters :as filters]
   [cheshire.core :as json]

   [clojure.spec.alpha :as s]
   [clojure.java.io :as io]))


(defn ->resource [path]
  (or
   (clojure.java.io/resource path)
   (throw (new Exception (format "Resource not found: %s" path)))))


(defn set-resource-path [path]
  (parser/set-resource-path!
   (->resource path)))


(filters/add-filter!
 :json-pretty
 (fn [data]
   (json/generate-string data {:pretty true})))


(defn slurp-resource [path]
  (-> path
      ->resource
      slurp))


(defn spec->schema [spec]
  (-> spec
      js/transform))


(defn remap-handler
  [[method handler]]

  (let [{:doc/keys [title
                    description
                    resource
                    ignore?]

         :handler/keys [spec-in
                        spec-out]} handler]

    {:method (-> method str (subs 1))
     :title title
     :description description
     :resource (some-> resource slurp-resource)
     :spec-in (some-> spec-in spec->schema)
     :spec-out (some-> spec-out spec->schema)}))


(defn ignored-handler?
  [[method handler]]
  (:doc/ignore? handler))


(def defaults
  {:doc/sorting :method})


(defn config->context [config]

  (let [config
        (->> defaults
             (config/rebase config)
             (s/assert ::spec.doc/config))

        {:doc/keys [title
                    description
                    resource
                    endpoint
                    sorting]
         :rpc/keys [handlers]} config]

    {:title title
     :description description
     :resource (some-> resource slurp-resource)
     :handlers
     (->> handlers
          (remove ignored-handler?)
          (mapv remap-handler)
          (sort-by sorting))}))


(defn generate-doc
  [config template outfile]
  (->> config
       config->context
       (parser/render-file template)
       (spit outfile)))



#_
(set-resource-path "templates")

#_
(generate-doc cfg
              "farseer/default.md"
              "test.md")
