(defproject puppetlabs/jruby-utils "5.2.1-SNAPSHOT"
  :description "A library for working with JRuby"
  :url "https://github.com/puppetlabs/jruby-utils"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :min-lein-version "2.9.1"
  :parent-project {:coords [puppetlabs/clj-parent "7.2.3"]
                   :inherit [:managed-dependencies]}

  :pedantic? :abort

  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :test-paths ["test/unit" "test/integration"]

  :dependencies [[org.clojure/clojure]
                 [org.clojure/java.jmx]
                 [org.clojure/tools.logging]

                 [clj-commons/fs]
                 [prismatic/schema]
                 [slingshot]

                 [puppetlabs/jruby-deps "9.4.8.0-1"]

                 [puppetlabs/i18n]
                 [puppetlabs/kitchensink]
                 [puppetlabs/trapperkeeper]
                 [puppetlabs/ring-middleware]]

  :deploy-repositories [["releases" {:url "https://clojars.org/repo"
                                     :username :env/clojars_jenkins_username
                                     :password :env/clojars_jenkins_password
                                     :sign-releases false}]]

  ;; By declaring a classifier here and a corresponding profile below we'll get an additional jar
  ;; during `lein jar` that has all the code in the test/ directory. Downstream projects can then
  ;; depend on this test jar using a :classifier in their :dependencies to reuse the test utility
  ;; code that we have.
  :classifiers [["test" :testutils]]

  :profiles {:dev {:dependencies  [[puppetlabs/kitchensink :classifier "test" :scope "test"]
                                   [puppetlabs/trapperkeeper :classifier "test" :scope "test"]
                                   [org.bouncycastle/bcpkix-jdk18on]
                                   [org.tcrawley/dynapath]]
                   :jvm-opts ~(let [version (System/getProperty "java.specification.version")
                                    [major minor _] (clojure.string/split version #"\.")]
                                (concat
                                  ["-Djruby.logger.class=com.puppetlabs.jruby_utils.jruby.Slf4jLogger"
                                   "-XX:+UseG1GC"
                                   "-Xms1G"
                                   "-Xmx2G"]
                                  (if (= 17 (java.lang.Integer/parseInt major))
                                    ["--add-opens" "java.base/sun.nio.ch=ALL-UNNAMED" "--add-opens" "java.base/java.io=ALL-UNNAMED"]
                                    [])))}
             :testutils {:source-paths ^:replace ["test/unit" "test/integration"]}}

  :plugins [[lein-parent "0.3.9"]
            [puppetlabs/i18n "0.9.2" :hooks false]])
