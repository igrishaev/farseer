(defproject com.github.igrishaev/farseer-client "0.1.2-SNAPSHOT"

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
   {:resource-paths ["../dev-resources"]
    :dependencies
    [[ch.qos.logback/logback-classic]]}

   :test
   {:resource-paths ["../dev-resources"]
    :dependencies
    [[com.github.igrishaev/farseer-jetty]
     [com.stuartsierra/component]]}}

  :dependencies
  [[com.github.igrishaev/farseer-common]

   [org.clojure/tools.logging]
   [clj-http]
   [cheshire]])
