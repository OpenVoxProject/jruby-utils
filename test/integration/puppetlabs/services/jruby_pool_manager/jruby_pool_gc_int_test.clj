(ns puppetlabs.services.jruby-pool-manager.jruby-pool-gc-int-test
  (:require [clojure.test :refer :all]
            [puppetlabs.services.jruby-pool-manager.jruby-testutils :as jruby-testutils]
            [puppetlabs.services.jruby-pool-manager.impl.jruby-internal :as jruby-internal])
  (:import (clojure.lang Agent)
           (java.lang.ref WeakReference)
           (java.util.concurrent ExecutorService Executors TimeUnit)))

;; This test asserts that org.jruby.Ruby runtimes become garbage collectable
;; once their pool has been flushed for shutdown. Getting a trustworthy
;; assertion requires controlling which threads execute Ruby code: every such
;; thread retains a SoftReference<ThreadContext> chaining to the runtime in
;; its ThreadLocalMap (see org.jruby.internal.runtime.ThreadService), the JVM
;; offers no way to force soft references clear short of memory pressure, and
;; a ThreadLocalMap only dies with its thread. So all Ruby-executing threads
;; must be terminated before the GC assertion:
;;
;;   * scriptlets, borrows, and the shutdown flush run on a disposable worker
;;     thread that is joined before asserting;
;;   * pool instance creation and cleanup run on the pool's internal
;;     creation-service executor, which is idle once the pool has been flushed
;;     for shutdown and is terminated explicitly;
;;   * the reference (multithreaded) pool fills itself on a Clojure agent
;;     send thread, so the global agent send executor is temporarily swapped
;;     for a disposable one that is terminated afterwards.

(def pool-size 2)

(defn trigger-java-integration!
  "Run a scriptlet on every instance in the pool that exercises behavior
  found to cause leaks. Currently, this helper creates instances of Java
  container classes and adds Ruby objects to them. This results in the
  JRuby runtime creating 'proxy classes' that mediate between Ruby and
  Java opbjects. The references held by these proxies can keep a JRuby
  instance alive after the pool is shut down."
  [pool-context]
  (jruby-testutils/reduce-over-jrubies!
   pool-context
   pool-size
   (constantly
    (str "require 'java'\n"
         "l = java.util.ArrayList.new\n"
         "l.add(1)\n"
         "java.lang.StringBuilder.new.append('x').to_s\n"))))

(defn weak-refs-to-pool-runtimes
  "Borrow every instance from the pool and return a vector of WeakReferences
  to their org.jruby.Ruby runtimes. Instances are returned to the pool, and no
  strong references to them escape this function."
  [pool-context]
  (let [instances (jruby-testutils/drain-pool pool-context pool-size)
        refs (mapv #(WeakReference. (jruby-internal/get-jruby-runtime %)) instances)]
    (jruby-testutils/fill-drained-pool pool-context instances)
    refs))

(defn shutdown-executor!
  [^ExecutorService executor]
  (.shutdown executor)
  (when-not (.awaitTermination executor 30 TimeUnit/SECONDS)
    (throw (IllegalStateException. "executor failed to terminate within 30s"))))

(defn run-pool-then-shutdown
  "Create a pool, exercise Java integration on every instance, and flush the
  pool for shutdown. Terminates the pool's creation-service executor once the
  pool is shut down. Returns WeakReferences to the pool's runtimes. Intended
  to run on a disposable thread; see the namespace comment."
  [config]
  (let [runtime-refs (atom [])
        creation-service (atom nil)]
    (jruby-testutils/with-pool-context
     pool-context
     jruby-testutils/default-services
     config
     (trigger-java-integration! pool-context)
     (reset! runtime-refs (weak-refs-to-pool-runtimes pool-context))
     (reset! creation-service (jruby-internal/get-creation-service pool-context)))
    ;; The pool has been flushed for shutdown, so its creation executor will
    ;; never receive work again; terminate its threads.
    (shutdown-executor! @creation-service)
    @runtime-refs))

(defn run-on-disposable-threads
  "Call f on a fresh worker thread, with the Clojure agent send executor
  swapped for a disposable one for the duration. Joins the worker and
  terminates the temporary executor before returning f's result, so that no
  thread that ran code on behalf of f survives."
  [f]
  (let [original-send-executor Agent/pooledExecutor
        temp-send-executor (Executors/newFixedThreadPool 2)
        result (atom nil)
        failure (atom nil)
        worker (Thread. (fn []
                          (try
                            (reset! result (f))
                            (catch Throwable t
                              (reset! failure t))))
                        "jruby-pool-gc-test-worker")]
    (set-agent-send-executor! temp-send-executor)
    (try
      (.start worker)
      (.join worker (* 5 60 1000))
      (when (.isAlive worker)
        (throw (IllegalStateException. "worker thread did not finish within 5 minutes")))
      (finally
        (set-agent-send-executor! original-send-executor)
        (shutdown-executor! temp-send-executor)))
    (when-let [t @failure]
      (throw t))
    @result))

(defn runtimes-collected-after-gc?
  "Request GCs until every WeakReference has been cleared, i.e. every runtime
  has been collected. Returns false if any runtime remains reachable."
  [runtime-refs]
  (jruby-testutils/wait-for-predicate
   (fn []
     (System/gc)
     (every? #(nil? (.get ^WeakReference %)) runtime-refs))
   ;; Loop 20 times, sleeping for 250 milliseconds between iterations.
   20
   250))

(defn reachable-runtime-count
  [runtime-refs]
  (count (remove #(nil? (.get ^WeakReference %)) runtime-refs)))

(deftest ^:integration jruby-runtimes-collected-after-pool-shutdown-test
  (doseq [[pool-flavor config-overrides] {"instance (singlethreaded) pool" {}
                                          "reference (multithreaded) pool" {:multithreaded true}}]
    (testing pool-flavor
      (let [config (jruby-testutils/jruby-config
                    (merge {:max-active-instances pool-size} config-overrides))
            runtime-refs (run-on-disposable-threads #(run-pool-then-shutdown config))]
        (is (= pool-size (count runtime-refs)))
        (is (runtimes-collected-after-gc? runtime-refs)
            (str "org.jruby.Ruby instances still reachable after pool shutdown: "
                 (reachable-runtime-count runtime-refs)
                 " of " (count runtime-refs) ". Terminated JRuby runtimes are"
                 " not being garbage collected; this indicates a significant"
                 " memory leak."))))))
