(ns ^{:author "Daniel Leong"
      :doc "Micro (but macro-based) logging framework"}
  wish.util.log
  (:require [clojure.string :as str]))

(defmacro -log
  "Actual internal logging macro"
  [level & args]
  (let [ns-part (-> *ns*
                    str
                    (str/replace "wish." "")
                    (str/split #"\.")
                    (->> (take-last 2)
                         (str/join ".")))
        log-fn (case level
                 :error 'js/console.error
                 :warn 'js/console.warn
                 'js/console.log)]
    `(~log-fn ~(str "[" ns-part "]") ~@args)))

(defmacro log
  "Simple verbose log macro; should get compiled out in a non-debug build"
  [& args]
  `(when js/goog.DEBUG
     (-log :log ~@args)))

(defmacro info
  [& args]
  `(-log :info ~@args))

(defmacro warn
  [& args]
  `(-log :warn ~@args))

(defmacro err
  [& args]
  `(-log :error ~@args))

(defmacro todo
  [& args]
  ; we probably don't need TODO messages in release builds
  `(log "TODO:" ~@args))

(defmacro make-fn
  "Declares a fn that logs with the given ns-part"
  [ns-part]
  `(fn ~(symbol ns-part)
     [& args#]
     (apply js/console.log ~(str "[" ns-part "]") args#)))
