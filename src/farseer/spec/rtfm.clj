(ns farseer.spec.rtfm
  (:require
   [clojure.string :as str]
   [clojure.spec.alpha :as s]))


(s/def :doc/title       string?)
(s/def :doc/description string?)
(s/def :doc/resource    string?)
(s/def :doc/ignore?     boolean?)
(s/def :doc/endpoint    string?)
(s/def :doc/sorting     #{:method :title})


(s/def ::config
  (s/keys :opt [:doc/title
                :doc/description
                :doc/resource
                :doc/ignore?
                :doc/endpoint
                :doc/sorting]))
