(ns farseer.spec.util
  (:require
   [clojure.string :as str]
   [clojure.spec.alpha :as s]))


(s/def ::spec
  (s/or :spec s/spec?
        :fn fn?
        :keyword qualified-keyword?
        :set set?))
