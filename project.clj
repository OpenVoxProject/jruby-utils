(def kitchensink-version "3.5.3")
(def trapperkeeper-version "4.3.0")
(def i18n-version "1.0.2")

(defproject org.openvoxproject/jruby-utils "5.3.2-SNAPSHOT"
  :description "A library for working with JRuby"
  :url "https://github.com/openvoxproject/jruby-utils"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :min-lein-version "2.9.1"

  :pedantic? :abort

  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :test-paths ["test/unit" "test/integration"]

  ;; These are to enforce consistent versions across dependencies of dependencies,
  ;; and to avoid having to define versions in multiple places. If a component
  ;; defined under :dependencies ends up causing an error due to :pedantic? :abort,
  ;; because it is a dep of a dep with a different version, move it here.
  :managed-dependencies [[org.clojure/clojure "1.12.4"]
                         [commons-io "2.21.0"]
                         [org.openvoxproject/kitchensink ~kitchensink-version]
                         [org.openvoxproject/kitchensink ~kitchensink-version :classifier "test"]
                         [org.openvoxproject/trapperkeeper ~trapperkeeper-version]
                         [org.openvoxproject/trapperkeeper ~trapperkeeper-version :classifier "test"]]

  :dependencies [[org.clojure/clojure]
                 [org.clojure/java.jmx "1.1.1"]
                 [org.clojure/tools.logging "1.3.1"]

                 [clj-commons/fs "1.6.312"]
                 [prismatic/schema "1.4.1"]
                 [slingshot "0.12.2"]
                 [ring/ring-core "1.15.3"]

                 [org.openvoxproject/jruby-deps "9.4.12.1-1"]

                 [org.openvoxproject/i18n ~i18n-version]
                 [org.openvoxproject/kitchensink]
                 [org.openvoxproject/trapperkeeper]
                 [org.openvoxproject/ring-middleware "2.1.0"]]

  :deploy-repositories [["releases" {:url "https://clojars.org/repo"
                                     :username :env/CLOJARS_USERNAME
                                     :password :env/CLOJARS_PASSWORD
                                     :sign-releases false}]]

  ;; By declaring a classifier here and a corresponding profile below we'll get an additional jar
  ;; during `lein jar` that has all the code in the test/ directory. Downstream projects can then
  ;; depend on this test jar using a :classifier in their :dependencies to reuse the test utility
  ;; code that we have.
  :classifiers [["test" :testutils]]

  :profiles {:dev {:dependencies  [[org.openvoxproject/kitchensink :classifier "test" :scope "test"]
                                   [org.openvoxproject/trapperkeeper :classifier "test" :scope "test"]
                                   [org.bouncycastle/bcpkix-jdk18on "1.83"]
                                   [org.tcrawley/dynapath "1.1.0"]]
                   :jvm-opts ~(let [version (System/getProperty "java.specification.version")
                                    [major minor _] (clojure.string/split version #"\.")]
                                (concat
                                  ["-Djruby.logger.class=com.puppetlabs.jruby_utils.jruby.Slf4jLogger"
                                   "-XX:+UseG1GC"
                                   "-Xms1G"
                                   "-Xmx2G"]
                                  (if (>= 17 (java.lang.Integer/parseInt major))
                                    ["--add-opens" "java.base/sun.nio.ch=ALL-UNNAMED" "--add-opens" "java.base/java.io=ALL-UNNAMED"]
                                    [])))}
             :testutils {:source-paths ^:replace ["test/unit" "test/integration"]}}

  :plugins [[org.openvoxproject/i18n ~i18n-version :hooks false]])
