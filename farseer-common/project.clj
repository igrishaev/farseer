(defproject com.github.igrishaev/farseer-common "0.1.3-SNAPSHOT"

  :description
  "Common parts of the project"

  :plugins [[lein-parent "0.3.8"]]

  :dependencies
  [[org.clojure/clojure]]

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
             [:profiles :dev]]})
