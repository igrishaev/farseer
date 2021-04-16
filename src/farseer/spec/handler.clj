(ns farseer.spec.handler
  (:require
   [clojure.string :as str]
   [clojure.spec.alpha :as s]))


(s/def :rpc/spec-validate-in?  boolean?)
(s/def :rpc/spec-validate-out? boolean?)
(s/def :rpc/batch-allowed?     boolean?)
(s/def :rpc/batch-max-size     nat-int?)
(s/def :rpc/batch-parallel?    boolean?)

(s/def :rpc/method qualified-keyword?)

(s/def :rpc/handlers
  (s/map-of :rpc/method :rpc/handler))


(s/def :handler/function
  (s/or :sym qualified-symbol?
        :fn fn?
        :var var?))

(s/def :handler/spec-in     qualified-keyword?)
(s/def :handler/spec-out    qualified-keyword?)
(s/def :handler/title       string?)
(s/def :handler/description string?)

(s/def :rpc/handler
  (s/keys :req [:handler/function]
          :opt [:handler/spec-in
                :handler/spec-out
                :handler/title
                :handler/description]))

(s/def ::config
  (s/keys
   :req [:rpc/handlers]
   :opt [:rpc/spec-validate-in?
         :rpc/spec-validate-out?
         :rpc/batch-allowed?
         :rpc/batch-max-size
         :rpc/batch-parallel?]))
