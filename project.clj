(defproject com.github.igrishaev/farseer-all "0.1.2-SNAPSHOT"

  :description
  "JSON-RPC client and server with tools"

  :url
  "https://github.com/igrishaev/farseer"

  :deploy-repositories
  {"releases" {:url "https://repo.clojars.org" :creds :gpg}}

  :license
  {:name "The Unlicense"
   :url "https://unlicense.org/"}

  :plugins
  [[lein-sub "0.3.0"]
   [exoscale/lein-replace "0.1.1"]]

  :sub ["farseer-common"
        "farseer-handler"
        "farseer-http"
        "farseer-jetty"
        "farseer-stub"
        "farseer-client"
        "farseer-doc"]

  :dependencies
  [[com.github.igrishaev/farseer-common]
   [com.github.igrishaev/farseer-handler]
   [com.github.igrishaev/farseer-http]
   [com.github.igrishaev/farseer-jetty]
   [com.github.igrishaev/farseer-stub]
   [com.github.igrishaev/farseer-client]
   [com.github.igrishaev/farseer-doc]]

  :managed-dependencies
  [[com.github.igrishaev/farseer-common :version]
   [com.github.igrishaev/farseer-handler :version]
   [com.github.igrishaev/farseer-http :version]
   [com.github.igrishaev/farseer-jetty :version]
   [com.github.igrishaev/farseer-stub :version]
   [com.github.igrishaev/farseer-client :version]
   [com.github.igrishaev/farseer-doc :version]

   [ch.qos.logback/logback-classic "1.2.3"]
   [cheshire "5.10.0"]
   [clj-http "3.12.0"]
   [com.stuartsierra/component "1.0.0"]
   [metosin/spec-tools "0.10.5"]
   [org.clojure/clojure "1.10.1"]
   [org.clojure/tools.logging "1.1.0"]
   [ring-basic-authentication "1.1.0"]
   [ring/ring-jetty-adapter "1.7.1"]
   [ring/ring-json "0.5.0"]
   [ring/ring-mock "0.4.0"]
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
   {:source-paths ["dev"]
    :dependencies [[ch.qos.logback/logback-classic]
                   [com.stuartsierra/component]]

    :global-vars {*assert* true
                  *warn-on-reflection* true}
    :jvm-opts ["-Dclojure.spec.compile-asserts=true"
               "-Dclojure.spec.check-asserts=true"]}

   :uberjar
   {:aot :all
    :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
