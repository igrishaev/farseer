(defproject com.github.igrishaev/farseer-doc "0.1.3-SNAPSHOT"

  :description
  "Utilities to generate RPC documentation"

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
    []}}

  :dependencies
  [[com.github.igrishaev/farseer-common]

   [metosin/spec-tools]
   [selmer]
   [cheshire]])
