(ns ^{:author "Daniel Leong"
      :doc "DataSource compiler"}
  wish.sources.compiler
  (:require [wish.sources.compiler.entity :refer [compile-entity]]
            [wish.sources.compiler.feature :refer [compile-feature]]
            [wish.sources.compiler.limited-use :refer [compile-limited-use]]
            [wish.sources.compiler.lists :refer [add-to-list inflate-items]]
            [wish.sources.core :refer [find-feature]]
            [wish.templ.fun :refer [->callable]]
            [wish.util :refer [->map]]))

; ======= options ==========================================

(defn compile-option
  [o]
  (if (contains? o :!)
    (compile-feature o)
    o))

; ======= directives =======================================

(def directives
  {:!add-limited-use
   (fn add-limited-use [state limited-use-map]
     (update state :limited-uses
             assoc
             (:id limited-use-map)
             (compile-limited-use limited-use-map)))

   :!add-to-list
   add-to-list

   :!declare-class
   (fn declare-class [state class-map]
     (update state :classes
             assoc
             (:id class-map) (compile-entity class-map)))

   :!declare-race
   (fn declare-race [state race-map]
     (update state :races
             assoc
             (:id race-map) (compile-entity race-map)))

   ; NOTE: this should never be applied top-level,
   ; but only to specific classes, races, etc.
   :!provide-attr
   (fn provide-attr [state id value]
     (assoc-in state [:attrs id] value))

   :!provide-feature
   (fn provide-feature [state & args]
     (let [features (->> args
                         (filter map?)
                         (map compile-feature)
                         ->map)
           filters (filter vector? args)]
       ; TODO apply filters?
       (update state :features
               merge features)))

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

(defn- install-deferred-options
  [s]
  (let [opts (:deferred-options s)]
    (reduce-kv
      (fn [state feature-id options]
        (update-in state [:features feature-id :values]
                   concat options))
      (dissoc s :deferred-options)
      opts)))

(defn- process-map
  "Call (processor s v) for each value in the map
   with key `k` in the state `s`"
  [k processor s]
  (update s k
          (fn [the-map]
            (reduce-kv
              (fn [result k v]
                (assoc result
                       k
                       (processor s v)))
              {}
              the-map))))

(declare apply-directive) ; part of the public API below
(defn- install-features
  [s entity]
  (-> entity
      (update :features
              (fn [features]
                (reduce-kv
                  (fn [m feature-id v]
                    (if (map? v)
                      m ; already inflated; do nothing

                      ; pull it out of the state
                      (assoc m feature-id
                             (get-in s [:features feature-id]))))
                  features
                  features)))
      ; apply features
      (as-> e (reduce
                apply-directive
                e
                (mapcat :! (:features e))))))

; ======= public api =======================================

(defn apply-directive [state directive-vector]
  (try
    (let [[kind & args] directive-vector]
      (if-let [f (get directives kind)]
        ; valid directive
        (apply f state args)

        ; unknown; ignore
        (do
          (println "WARN: unknown directive:" kind)
          state)))
    (catch js/Error e
      (throw (js/Error.
               (str "Error processing " directive-vector
                    "\n\nOriginal error: " e))))))

(defn compile-directives
  "Given a sequence of directives, return a compiled DataSource state"
  [directives]
  (->> directives

       ; compile directives into a state
       (reduce apply-directive {})

       ; deferred processing, now that all directives have been applied
       ; and all features/options should be available
       install-deferred-options

       (process-map :classes install-features)
       (process-map :races install-features)
       (process-map :lists inflate-items)

       ))

(defn- apply-feature-options
  [data-source state feature-id options-chosen]
  (if (empty? options-chosen)
    state

    (let [option-value (first options-chosen)
          feature-directives (:! (find-feature data-source option-value))]
      (recur
        data-source

        ; new state:
        (if feature-directives
          (reduce apply-directive state feature-directives)

          (do
            (println "TODO apply " option-value " for feature " feature-id)
            state))

        feature-id
        (next options-chosen)))))

(defn apply-options
  [data-source state options-map]
  (if (empty? options-map)
    state

    (let [[feature-id options-chosen] (first options-map)]
      (recur
        data-source

        (apply-feature-options data-source state feature-id options-chosen)
        (next options-map)))))
