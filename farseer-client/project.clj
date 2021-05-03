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
             :url]}

  :profiles
  {:dev
   {:resource-paths ["../dev-resources"]
    :dependencies
    [[ch.qos.logback/logback-classic]
     [farseer-jetty]
     [com.stuartsierra/component]]}}

  :dependencies
  [[org.clojure/tools.logging]

   [farseer-common]

   [clj-http]
   [cheshire]])
