(defproject farseer-all "0.1.0-SNAPSHOT"

  :description "JSON-RPC client and server with tools"

  :url "https://github.com/igrishaev/farseer"

  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}

  :plugins
  [[lein-sub "0.3.0"]
   [exoscale/lein-replace "0.1.1"]]

  :sub ["farseer-handler"
        "farseer-client"
        "farseer-stub"]

  :dependencies
  [[farseer-handler]
   [farseer-client]
   [farseer-stub]]

  :managed-dependencies
  [[farseer-handler :version]
   [farseer-client :version]
   [farseer-stub :version]

   [org.clojure/clojure "1.10.1"]
   [org.clojure/tools.logging "1.1.0"]

   [clj-http "3.12.0"]
   [cheshire "5.10.0"]

   [ring/ring-jetty-adapter "1.7.1"]
   [ring/ring-json "0.5.0"]

   [ch.qos.logback/logback-classic "1.2.3"]]

  :release-tasks
  [["vcs" "assert-committed"]
   ["sub" "change" "version" "leiningen.release/bump-version" "release"]
   ["change" "version" "leiningen.release/bump-version" "release"]
   ["vcs" "commit"]
   ["vcs" "tag" "--no-sign"]
   ["sub" "with-profile" "uberjar" "install"]
   ["sub" "with-profile" "uberjar" "deploy"]
   ["deploy"]
   ["sub" "change" "version" "leiningen.release/bump-version"]
   ["change" "version" "leiningen.release/bump-version"]
   ["vcs" "commit"]
   ["vcs" "push"]]

  :test-selectors
  {:all         (constantly true)
   :default     (complement :integration)
   :integration :integration}

  :profiles
  {:provided
   {:dependencies [[org.clojure/clojure]]}

   :dev
   {:global-vars {*assert* true
                  *warn-on-reflection* true}}
   :uberjar
   {:aot :all
    :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
