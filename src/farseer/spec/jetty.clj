(ns farseer.spec.jetty
  (:require
   [clojure.string :as str]
   [clojure.spec.alpha :as s]))


(s/def :jetty/port  pos-int?)
(s/def :jetty/join? boolean?)


(s/def ::config
  (s/keys :req [:jetty/port
                :jetty/join?]))
