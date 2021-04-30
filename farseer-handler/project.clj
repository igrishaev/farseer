(defproject farseer-handler "0.1.0-SNAPSHOT"

  :description
  "The basic, transport-agnostic RPC handler"

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
             [:profiles :dev]
             [:profiles :test :plugins]]}

  :profiles
  {:dev
   {:resource-paths ["../dev-resources"]
    :dependencies
    [[ch.qos.logback/logback-classic]]}}

  :dependencies
  [[org.clojure/tools.logging]])
