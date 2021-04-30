(ns farseer.spec.rtfm
  (:require
   [farseer.spec.util :as util]
   [clojure.string :as str]
   [clojure.spec.alpha :as s]))


(s/def :doc/title       string?)
(s/def :doc/description string?)
(s/def :doc/resource    string?)
(s/def :doc/ignore?     boolean?)
(s/def :doc/endpoint    string?)
(s/def :doc/sorting     #{:method :title})


(s/def :doc/handler
  (s/keys :opt [:doc/title
                :doc/description
                :doc/resource
                :doc/ignore?
                :handler/spec-in
                :handler/spec-out]))


(s/def :handler/spec-in  ::util/spec)
(s/def :handler/spec-out ::util/spec)


(s/def :rpc/handlers
  (s/map-of keyword? :doc/handler))


(s/def ::config
  (s/keys :req [:rpc/handlers]
          :opt [:doc/title
                :doc/description
                :doc/resource
                :doc/endpoint
                :doc/sorting]))
