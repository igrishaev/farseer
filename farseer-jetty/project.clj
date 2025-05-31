(defproject com.github.igrishaev/farseer-jetty "0.1.2-SNAPSHOT"

  :description
  "Jetty server for HTTP RPC handler"

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
    [[com.stuartsierra/component]
     [clj-http]
     [ring-basic-authentication]]}}

  :dependencies
  [[com.github.igrishaev/farseer-http]

   [ring/ring-jetty-adapter]])
