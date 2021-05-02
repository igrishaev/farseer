(defproject farseer-client "0.1.0-SNAPSHOT"

  :description
  "HTTP client for RPC server"

  :plugins [[lein-parent "0.3.8"]]

  :parent-project
  {:path "../project.clj"
   :inherit [:deploy-repositories
             :license
             :managed-dependencies
             :plugins
             :repositories
             :scm
             :test-selectors
             :url
             [:profiles :dev]]}

  :profiles
  {:dev
   {:dependencies
    [[ch.qos.logback/logback-classic]
     [farseer-stub]]}}

  :dependencies
  [[org.clojure/tools.logging]
   [clj-http]
   [cheshire]])
