(defproject farseer-doc "0.1.0"

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
  [[farseer-common]

   [metosin/spec-tools]
   [selmer]
   [cheshire]])
