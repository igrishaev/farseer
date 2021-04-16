(ns farseer.spec.stub
  (:require
   [clojure.string :as str]
   [clojure.spec.alpha :as s]))


(def ex? (partial instance? Throwable))


(s/def :stub/result
  (s/or :fn fn?
        :ex ex?
        :data any?))


(s/def :stub/handlers
  (s/map-of :rpc/method :stub/result))
