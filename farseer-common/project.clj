(defproject com.github.igrishaev/farseer-common "0.1.2-SNAPSHOT"

  :description
  "Common parts of the project"

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
             [:profiles :dev]]})
