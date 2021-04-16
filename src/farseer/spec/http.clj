(ns farseer.spec.http
  (:require
   [clojure.string :as str]
   [clojure.spec.alpha :as s]))


(s/def :http/method #{:post :get :put :delete})


(defn path? [line]
  (str/starts-with? line "/"))


(s/def :http/path
  (s/and string? path?))


(s/def :http/health? boolean?)


(s/def ::config
  (s/keys :req [:http/method
                :http/path
                :http/health?]))
