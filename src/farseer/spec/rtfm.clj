(ns farseer.spec.rtfm
  (:require
   [clojure.string :as str]
   [clojure.spec.alpha :as s]))


(s/def :doc/title       string?)
(s/def :doc/description string?)
(s/def :doc/file        string?)
(s/def :doc/ignore?     boolean?)
(s/def :doc/endpoint    string?)


#_
(s/def :rpc/handler
  (s/merge
   :rpc/handler
   (s/keys :opt [:doc/title
                 :doc/description
                 :doc/file
                 :doc/ignore?])))





(s/def ::config
  (s/keys :opt [:doc/title
                :doc/description
                :doc/file
                :doc/endpoint]))
