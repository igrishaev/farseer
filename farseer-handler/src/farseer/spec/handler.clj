(ns farseer.spec.handler
  (:require
   [clojure.string :as str]
   [clojure.spec.alpha :as s]))


(s/def ::ne-string
  (s/and string? (complement str/blank?)))


(s/def ::id
  (s/or :int int? :string ::ne-string))


(s/def ::version #{"2.0"})


(s/def ::method
  (s/or :keyword keyword? :string ::ne-string))


(s/def ::params-map
  (s/map-of keyword? any?))


(s/def ::params
  (s/or :map ::params-map :vec vector?))


(s/def ::rpc-single
  (s/keys :req-un [::version
                   ::method]
          :opt-un [::id
                   ::params]))


(s/def ::rpc-batch
  (s/coll-of ::rpc-single))


(s/def ::rpc
  (s/or :single ::rpc-single :batch ::rpc-batch))
