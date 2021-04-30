(ns farseer.rtfm-test
  (:require
   [farseer.rtfm :as rtfm]

   [clojure.spec.alpha :as s]
   [clojure.test :refer [deftest is]]))


(s/def :api/message string?)


(def config
  {:doc/title "My API"
   :doc/description "Long API Description"

   :rpc/handlers
   {
    :user/delete
    {:doc/title "Delete a user by ID"
     :doc/description "Long text for deleting a user."
     :handler/spec-in pos-int?
     :handler/spec-out (s/keys :req-un [:api/message])}

    :user/get-by-id
    {:doc/title "Get a user by ID"
     :doc/description "Long text for getting a user."
     :doc/ignore? false
     :doc/resource "docs/user-get-by-id.md"
     :handler/spec-in int?
     :handler/spec-out
     (s/map-of keyword? (s/or :int int? :str string?))}

    :hidden/api
    {:doc/title "Non-documented API"
     :doc/ignore? true
     :handler/spec-in any?
     :handler/spec-out any?}}})


(deftest test-context-ok

  (let [context (rtfm/config->context config)]

    (is (=

         '
         {:title "My API"
          :description "Long API Description"
          :resource nil
          :handlers
          ({:method "user/delete"
            :title "Delete a user by ID"
            :description "Long text for deleting a user."
            :resource nil
            :spec-in {:type "integer" :format "int64" :minimum 1}
            :spec-out
            {:type "object"
             :properties {"message" {:type "string"}}
             :required ["message"]}}
           {:method "user/get-by-id"
            :title "Get a user by ID"
            :description "Long text for getting a user."
            :resource
            "\n### Get user by ID examples\n\n```bash\nFOO=42 do this\n```\n\n- one example\n- another example\n- test\n\n[link]: test.com\n\nCheck this [link][link] for more info.\n"
            :spec-in {:type "integer" :format "int64"}
            :spec-out
            {:type "object"
             :additionalProperties
             {:anyOf [{:type "integer" :format "int64"} {:type "string"}]}}})}

         context))))


(deftest test-broken-config

  (let [config*
        (assoc-in config [:rpc/handlers :user/delete :doc/title] 42)]

    (is (thrown?
         Exception
         (rtfm/config->context config*)))))
