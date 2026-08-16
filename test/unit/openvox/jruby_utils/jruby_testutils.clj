(ns openvox.jruby-utils.jruby-testutils
  "Utility functions for JRuby tests."
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (java.net URLClassLoader URL)))

(defn with-restored-system-properties
  "Creates a test fixture that looks up named Java properties, caches their
  values, runs the test case, then restores any cached values."
  [& properties]
  (fn [f]
    (let [saved-properties (into {} (map #(do [% (System/getProperty %)])) properties)]
      (try
        (f)
        (finally
          (doseq [[k v] saved-properties]
            (if v
              (System/setProperty k v)
              (System/clearProperty k))))))))


(def ^:dynamic *loader* nil)

(defn gen-isolated-loader
  "Create an isolated class loader, populated with a copy of the JVM
  classpath."
  ^URLClassLoader []
  (->> (str/split (System/getProperty "java.class.path")
                   (re-pattern (System/getProperty "path.separator")))
       (map #(-> (io/file %) .toURI .toURL))
       (into-array URL)
       ;; The last argument, nil parent, prevents delegation to the system
       ;; classloader, ensuring true isolation
       (#(URLClassLoader. % nil))))

(defn with-isolated-classloader
  "Creates a temporary Java classloader, assigns it to *loader* and then
  runs the test case with that variable in-context. This enables testing
  class variables that are initialized at load-time with different
  system property combinations. Java reflection must be used to invoke
  constructors and other static methods of classes loaded into the temporary
  classloader."
  [f]
  (with-open [loader (gen-isolated-loader)]
    (binding [*loader* loader]
      (f))))
