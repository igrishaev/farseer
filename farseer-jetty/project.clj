(defproject farseer-jetty "0.1.0-SNAPSHOT"

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
             :url]}

  :profiles
  {:dev
   {:resource-paths ["../dev-resources"]
    :dependencies
    [[ch.qos.logback/logback-classic]

     [com.stuartsierra/component]
     [clj-http]
     [ring-basic-authentication]]}}

  :dependencies
  [[farseer-http]

   [ring/ring-jetty-adapter]])
