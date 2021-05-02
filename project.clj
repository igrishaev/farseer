(defproject farseer-all "0.1.0-SNAPSHOT"

  :description "JSON-RPC client and server with tools"

  :url "https://github.com/igrishaev/farseer"

  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}

  :plugins
  [[lein-sub "0.3.0"]
   [exoscale/lein-replace "0.1.1"]]

  :sub ["farseer-handler"
        "farseer-http"
        "farseer-jetty"
        "farseer-stub"

        "farseer-client"
        "farseer-doc"]

  :dependencies
  [

   ;; [farseer-handler]
   ;; [farseer-http]
   ;; [farseer-jetty]
   ;; [farseer-stub]

   ;; [farseer-client]
   ;; [farseer-doc]

   ;; [org.clojure/clojure]


   ;; [ring/ring-json]


   [org.clojure/tools.logging]
   [clj-http]
   [cheshire]

   [ring/ring-jetty-adapter]


   [ch.qos.logback/logback-classic]
   [com.stuartsierra/component]
   [ring-basic-authentication]
   [metosin/spec-tools]
   [selmer]]

  :managed-dependencies
  [[farseer-handler :version]
   [farseer-http :version]
   [farseer-jetty :version]
   [farseer-stub :version]

   [farseer-client :version]
   [farseer-doc :version]


   [org.clojure/clojure "1.10.1"]
   [org.clojure/tools.logging "1.1.0"]


   [ring/ring-jetty-adapter "1.7.1"]

   [ring/ring-mock "0.4.0"]

   [clj-http "3.12.0"]
   [cheshire "5.10.0"]


   [ring/ring-json "0.5.0"]

   [ch.qos.logback/logback-classic "1.2.3"]

   [com.stuartsierra/component "1.0.0"]
   [ring-basic-authentication "1.1.0"]
   [metosin/spec-tools "0.10.5"]
   [selmer "1.12.34"]]

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
                  *warn-on-reflection* true}
    :jvm-opts ["-Dclojure.spec.compile-asserts=true"
               "-Dclojure.spec.check-asserts=true"]}

   :uberjar
   {:aot :all
    :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
