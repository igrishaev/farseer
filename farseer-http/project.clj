(defproject com.github.igrishaev/farseer-http "0.1.2"

  :description
  "HTTP Ring handler for an RPC hadnler"

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
    [[ch.qos.logback/logback-classic]]}}

  :dependencies
  [[com.github.igrishaev/farseer-handler]

   [ring/ring-mock]
   [ring/ring-json]])
