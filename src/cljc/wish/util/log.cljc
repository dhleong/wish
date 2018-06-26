(ns ^{:author "Daniel Leong"
      :doc "Micro (but macro-based) logging framework"}
  wish.util.log
  (:require [clojure.string :as str]))

(defmacro -log
  "Actual internal logging macro. opts can be just a level"
  [opts args]
  (let [level (if (keyword? opts)
                opts
                (:level opts))
        ns-part (if (keyword? opts)
                  (-> *ns*
                      str
                      (str/replace "wish." "")
                      (str/split #"\.")
                      (->> (take-last 2)
                           (str/join ".")))
                  (:ns opts))
        log-fn (case level
                 :error 'js/console.error
                 :warn 'js/console.warn
                 'js/console.log)]

    ; NOTE: on release builds we don't have the devtools to nicely format
    ; cljs types, so we should stringify params. It'd be nice if we could
    ; do this at compile time instead of runtime, but I can't seem to find
    ; a way to determine whether we're in debug mode or not at compile time...
    `(let [args# ~(if (symbol? args)
                    args         ; we were given a var that contains a list already
                    (vec args))  ; normal macro use
           args# (if js/goog.DEBUG
                  args#
                  (map str args#))]
       (apply ~log-fn ~(str "[" ns-part "]") args#))))

(defmacro log
  "Simple verbose log macro; should get compiled out in a non-debug build"
  [& args]
  `(when js/goog.DEBUG
     (-log :log ~args)))

(defmacro info
  [& args]
  `(-log :info ~args))

(defmacro warn
  [& args]
  `(-log :warn ~args))

(defmacro err
  [& args]
  `(-log :error ~args))

(defmacro todo
  [& args]
  ; we probably don't need TODO messages in release builds
  `(log "TODO:" ~@args))

(defmacro make-fn
  "Declares a fn that logs with the given ns-part"
  [ns-part]
  `(fn ~(symbol ns-part)
     [& args#]
     (-log {:ns ~ns-part
            :level :info} args#)))
