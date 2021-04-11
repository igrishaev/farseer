(defproject farseer-stub "0.1.0-SNAPSHOT"

  :plugins [[lein-parent "0.3.8"]]

  :parent-project
  {:path "../project.clj"
   :inherit [:repositories :scm :deploy-repositories
             :managed-dependencies :description :url :license
             :plugins :test-selectors [:profiles :test :plugins]]}

  :profiles
  {:dev
   {:dependencies
    [[ch.qos.logback/logback-classic]]}}

  :dependencies
  [[farseer-handler]
   [org.clojure/tools.logging]
   [ring/ring-jetty-adapter]
   [ring/ring-json]])
