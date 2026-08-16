(ns openvox.jruby-utils.jruby-defaults
  "Requiring this namespace has one side-effect: it sets Java properties
  read by JRuby settings in order to change default behavior, while retaining
  the ability for users to explicitly overrride settings via JAVA_ARGS.

  This namespace should be required very early in the application lifecycle,
  before any classes are imported from org.jruby, as some properties are read
  once during class initialization and then never consulted again.")

(def defaults
  "Map of String->String indicating defaults to set in JVM properties that
  influence JRuby behavior."
  {
    ;; JRuby 9.4 defaulted to false, JRuby 10.0 changes to true. Using
    ;; InvokeDynamic can ultimately produce faster code, but this comes at the
    ;; cost of a slower, more volatile warm-up period. So, keep the 9.4 default
    ;; of false as OpenVox continually creates anonymous Ruby classes that
    ;; repeatedly trigger expensive compilation.
    ;;
    ;; Re-visit if improvements are made on the JRuby side to reduce cost, or
    ;; on the OpenVox side to reduce the number of ephemeral Ruby classes that
    ;; force the JRuby compiler to repeatedly re-compile.
    "jruby.compile.invokedynamic" "false"
  })

(defn set-jruby-property-defaults!
  "Loop over the defaults map and set each in Java properties, if not already
  set by some other mechanism like a `-D` flag. Return a map of properties
  set in this way."
  []
  (let [properties (System/getProperties)]
    (into {} (filter (fn [[k v]] (nil? (.putIfAbsent properties k v))) defaults))))

;; This statement causes the actual side-effect of setting defaults in the Java
;; system properties when this namespace is loaded. The variable holds a map of
;; properties that were modified.
(defonce defaults-set-on-load (set-jruby-property-defaults!))
