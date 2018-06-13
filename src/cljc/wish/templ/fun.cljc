(ns ^{:author "Daniel Leong"
      :doc "Macros for exposing fns to templat functions"}
  wish.templ.fun
  (:require [clojure.string :as str]))

; NOT exhaustive, but should cover our uses:
(def reserved?
  #{"if" "let" "for"})

(defn ->js-name
  [n]
  (if (reserved? n)
    (str n "$")
    (str/replace
      n
      #"[+-/*?<>]"
      (fn [ch]
        (case ch
          "+" "_PLUS_"
          "-" "_"
          "/" "_SLASH_"
          "*" "_STAR_"
          "?" "_QMARK_"
          ">" "_GT_"
          "<" "_LT_")))))

(defmacro expose-fn
  [m fn-symbol & [run-on-args]]
  (let [n (name fn-symbol)
        this-ns-name (name (ns-name *ns*))
        exported-name (str "exported-" n)
        exported-symbol (symbol (str "exported-"
                                     (str/replace n #"/" "_SLASH_")))
        js-name (str this-ns-name "."
                     (->js-name exported-name))
        core-ns (-> fn-symbol resolve meta :ns ns-name name)
        core-ns-symbol (symbol core-ns n)]
    `(as-> ~m ~'m
       ;; (println ~(meta (resolve fn-symbol)))
       (do
         (defn ^:export ~exported-symbol
           [& ~'args]
           ~(if run-on-args
              `(apply ~core-ns-symbol (~run-on-args ~'args))
              `(apply ~core-ns-symbol ~'args)))
         (when-not js/goog.DEBUG
           (~'js/goog.exportSymbol ~js-name ~exported-symbol))
         (assoc ~'m (symbol ~n) (symbol
                                  ~this-ns-name
                                  ~(name exported-symbol)))))))

(defmacro export-macro
  "Ensure a cljs.core macro is exported"
  [macro-sym & [conditional?]]
  (let [export `(~'js/goog.exportSymbol
                  ~(str "cljs.core$macros."
                        (->js-name (name macro-sym)))
                  ~(symbol (str "cljs.core$macros/"
                                (name macro-sym))))]
    (if conditional?
      `(when-not js/goog.DEBUG
         ~export)
      export)))

(defmacro export-sym
  [sym]
  (let [n (name sym)
        core-ns (if-let [sym-meta (-> sym resolve meta)]
                  (-> sym-meta :ns ns-name name)
                  "cljs.core")]
    `(~'js/goog.exportSymbol
       ~(str core-ns "." (->js-name n))
       ~(symbol core-ns n))))

