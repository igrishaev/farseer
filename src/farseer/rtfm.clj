(ns farseer.rtfm
  (:require
   [spec-tools.json-schema :as js]
   [selmer.parser :as parser]
   [selmer.filters :as filters]
   [cheshire.core :as json]

   [clojure.spec.alpha :as s]
   [clojure.java.io :as io]))


(parser/set-resource-path!
 (clojure.java.io/resource "templates"))


(filters/add-filter!
 :json-pp
 (fn [data]
   (json/generate-string data {:pretty true})))


(defn config->context [config]

  (let [{:doc/keys [title
                    description
                    resource
                    endpoint]} config]

    {:title title
     :description description
     :resource (some-> resource io/resource slurp)
     :handlers
     (for [[method handler] (:rpc/handlers config)
           :when (not (:doc/ignore? handler))]

       (let [{:doc/keys [title
                         description
                         resource
                         ignore?]

              :handler/keys [spec-in
                             spec-out]} handler]

         {:method (-> method str (subs 1))
          :title title
          :description description
          :resource (some-> resource io/resource slurp)
          :spec-in (when spec-in
                     (js/transform spec-in))

          :spec-out (when spec-out
                      (js/transform spec-out))}))})  )



(def cfg
  {:doc/title "My API"
   :doc/description "Long API Description"

   :rpc/handlers
   {:user/get-by-id
    {:doc/title "Get a user by ID"
     :doc/description "Long text for getting a user."
     :doc/ignore? false
     :doc/resource "docs/user-get-by-id.md"
     :handler/spec-in (s/tuple int? int?)
     :handler/spec-out (s/map-of keyword? int?)}

    :user/delete
    {:doc/title "Delete a user by ID"
     :doc/description "Long text for deleting a user."
     :doc/ignore? false
     :handler/spec-in pos-int?
     :handler/spec-out (s/map-of keyword? int?)}}})


;; sorting
;; tests
;; add to examples
;; move resources
;; decide on spec

(defn generate-doc
  [config template]
  (parser/render-file template (config->context config)))


#_
(spit "test.md"
      (generate-doc cfg "json-rpc-api.md"))
