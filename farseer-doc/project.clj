(defproject farseer-doc "0.1.0-SNAPSHOT"

  :description
  "Utilities to generate RPC documentation"

  :plugins [[lein-parent "0.3.8"]]

  :parent-project
  {:path "../project.clj"
   :inherit [:repositories :scm :deploy-repositories
             :managed-dependencies :url :license
             :plugins :test-selectors [:profiles :test :plugins]]}

  :profiles
  {:dev
   {:dependencies
    []}}

  :dependencies
  [])
