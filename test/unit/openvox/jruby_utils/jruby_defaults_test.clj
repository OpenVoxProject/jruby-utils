(ns openvox.jruby-utils.jruby-defaults-test
  (:require [clojure.test :refer :all]
            [openvox.jruby-utils.jruby-testutils :as testutils]
            [openvox.jruby-utils.jruby-defaults :as jruby-defaults]))

(use-fixtures :each (apply testutils/with-restored-system-properties
                      (keys jruby-defaults/defaults)))

(deftest defaults-respect-explicit-settings
  (testing "Does not override jruby.compile.invokedynamic when explicitly set"
    (System/setProperty "jruby.compile.invokedynamic" "true")
    (is (= {} (jruby-defaults/set-jruby-property-defaults!)))
    (is (= "true" (System/getProperty "jruby.compile.invokedynamic")))))

(deftest defaults-set-for-null-settings
  (testing "Sets jruby.compile.invokedynamic to false if not already set."
    (System/clearProperty "jruby.compile.invokedynamic")
    (is (= {"jruby.compile.invokedynamic" "false"}
           (jruby-defaults/set-jruby-property-defaults!)))
    (is (= "false" (System/getProperty "jruby.compile.invokedynamic")))))
