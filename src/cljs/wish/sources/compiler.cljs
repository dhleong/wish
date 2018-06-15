(ns ^{:author "Daniel Leong"
      :doc "DataSource compiler"}
  wish.sources.compiler
  (:require [wish.sources.compiler.feature :refer [compile-feature]]
            [wish.templ.fun :refer [->callable]]))

; ======= options ==========================================

(defn compile-option
  [o]
  (if (contains? o :!)
    (compile-feature o)
    o))

; ======= directives =======================================

(def directives
  {:!declare-class
   (fn declare-class [state class-map]
     (update state :classes
             assoc
             (:id class-map) class-map))

   :!provide-feature
   (fn provide-feature [state & args]
     (let [features (->> args
                         (filter map?)
                         (map compile-feature))
           filters (filter vector? args)]
       ; TODO apply filters?
       (->> features
            (reduce
              (fn [s feature-map]
                (update s :features
                        assoc
                        (:id feature-map) feature-map))
              state))))

   :!provide-options
   (fn provide-options [state feature-id & option-maps]
     (let [options (map compile-option option-maps)]
       (if (get-in state [:features feature-id])
         (update-in state [:features feature-id :values]
                    concat options)

         ;; TODO
         (update-in state [:deferred-options feature-id]
                    concat options))))
   })

(defn apply-directive [state directive-vector]
  (let [[kind & args] directive-vector]
    (if-let [f (get directives kind)]
      ; valid directive
      (apply f state args)

      ; unknown; ignore
      (do
        (println "WARN: unknown directive:" kind)
        state))))

(defn- install-deferred-options
  [s]
  (let [opts (:deferred-options s)]
    (reduce
      (fn [state [feature-id options]]
        (update-in state [:features feature-id :values]
                   concat options))
      (dissoc s :deferred-options)
      opts)))

; ======= public api =======================================

(defn compile-directives
  "Given a sequence of directives, return a compiled DataSource state"
  [directives]
  (->> directives

       ; compile directives into a state
       (reduce apply-directive {})

       ; install options
       install-deferred-options

       ; TODO more deferred processing, now that we have all directives
       ; EG: provide all filtered features to matching classes
       ))

