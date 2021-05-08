(defproject com.github.igrishaev/farseer-stub "0.1.1-SNAPSHOT"

  :description
  "Local HTTP stub for RPC server"

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
    [[ch.qos.logback/logback-classic]]}}

  :dependencies
  [[com.github.igrishaev/farseer-common]
   [com.github.igrishaev/farseer-jetty]

   [org.clojure/tools.logging]])
