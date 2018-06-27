(ns ^{:author "Daniel Leong"
      :doc "DataSource compiler"}
  wish.sources.compiler
  (:require-macros [wish.util.log :as log])
  (:require [clojure.data :refer [diff]]
            [wish.sources.compiler.entity :refer [compile-entity]]
            [wish.sources.compiler.entity-mod :refer [apply-entity-mod]]
            [wish.sources.compiler.feature :refer [compile-feature]]
            [wish.sources.compiler.fun :refer [->callable]]
            [wish.sources.compiler.limited-use :refer [compile-limited-use]]
            [wish.sources.compiler.lists :refer [add-to-list inflate-items]]
            [wish.sources.core :refer [find-feature]]
            [wish.util :refer [->map process-map]]))

; ======= constants ========================================

; wish compat version number for new character sheets
(def compiler-version 1)

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
     (let [path (if (keyword? id)
                  [:attrs id]
                  (cons :attrs id))]
       (assoc-in state path value)))

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
     ; TODO should this accept option ids as well?
     (let [options (map compile-option option-maps)

           ; install the options as features
           state (update state :features merge (->map options))]
       (if (get-in state [:features feature-id])
         (update-in state [:features feature-id :values]
                    concat options)

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

(declare apply-directive) ; part of the public API below
(defn- install-features
  ([s entity]
   (install-features s entity nil))
  ([s entity data-source]
   (-> entity
       (update :features
               (fn [features]
                 (reduce-kv
                   (fn [m feature-id v]
                     (if (map? v)
                       ; ensure it's compiled
                       (assoc m feature-id (compile-feature v))

                       ; pull it out of the state (or data source,
                       ; if we have one)
                       (assoc m feature-id
                              (or (get-in s [:features feature-id])
                                  (when data-source
                                    (find-feature data-source feature-id))))))
                   features
                   features)))
       ; apply features
       (as-> e (reduce
                 apply-directive
                 e
                 (mapcat :! (vals (:features e))))))))

; ======= public api =======================================

(defn apply-directive [state directive-vector]
  (try
    (let [[kind & args] directive-vector]
      (if-let [f (get directives kind)]
        ; valid directive
        (apply f state args)

        ; unknown; ignore
        (do
          (log/warn "unknown directive:" kind)
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


; ======= Options ==========================================

(defn- apply-feature-options
  [data-source state feature-id options-chosen]
  (if (or (empty? options-chosen)

          ; don't apply any options if the feature doesn't apply to this state
          ; (EX: racial feature options on the class, or vice versa)
          (not (get-in state [:features feature-id])))
    state

    (let [option-value (first options-chosen)
          feature-directives (:! (find-feature data-source option-value))]
      (recur
        data-source

        ; new state:
        (if feature-directives
          (reduce apply-directive state feature-directives)

          (do
            ; NOTE: at this point, there may just be no directives to apply?
            (log/warn "failed to apply " option-value " for feature " feature-id)
            state))

        feature-id
        (next options-chosen)))))

(defn apply-options
  [state data-source options-map]
  (if (empty? options-map)
    state

    (let [[feature-id options-chosen] (first options-map)]
      (recur
        (apply-feature-options data-source state feature-id options-chosen)

        data-source
        (next options-map)))))


; ======= Level-scaling ====================================

(defn- get-scaling
  [context k path]
  (when-let [scaling (k context)]
    [scaling path]))

(defn- find-level-scaling
  [state k]
  (-> (get-scaling state k nil)
      (cons (map
              (fn [[id feature]]
                (get-scaling feature k [:features id]))
              (:features state)))
      (->> (filter identity))))

(defn- apply-mod-in
  "Apply mod-map in `path` and install newly-added features"
  [state data-source mod-map path]
  (let [after (if path
                (update-in state path apply-entity-mod mod-map)
                (apply-entity-mod state mod-map))
        ; only install features added
        [_ added _] (diff state after)
        new-installed (when added
                        (install-features state added data-source))]
    (merge-with merge after new-installed)))

(defn- apply-scaling-for-level
  [state path this-scaling data-source level]
  (if-let [values (get this-scaling level)]
    (apply-mod-in
      state data-source
      values path)

    ; no scaling; pass through
    state))

(defn- apply-levels-with
  [original-state data-source levels-key apply-fn]
  (loop [state original-state
         level (:level state)
         merge-scaling (find-level-scaling state levels-key)]
    (if-let [[this-scaling path] (first merge-scaling)]
      (recur
        (apply-fn
          state path
          this-scaling data-source
          level)

        level
        (next merge-scaling))

      ; done!
      state)))

(defn- apply-all-levels
  "Apply :&levels statements in `original-state`"
  [original-state data-source]
  (apply-levels-with
    original-state
    data-source
    :&levels
    (fn [state path this-scaling data-source level]
      (reduce
        (fn [s apply-level]
          (apply-scaling-for-level
            s path
            this-scaling data-source
            apply-level))
        state
        (range 1 (inc level))))))

(defn- apply-current-level
  "Applies :levels statements in `original-state`"
  [original-state data-source]
  (apply-levels-with
    original-state
    data-source
    :levels
    apply-scaling-for-level))

(defn apply-levels
  [state data-source]
  (-> state
      (apply-current-level data-source)
      (apply-all-levels data-source)))


; ======= Public interface =================================

(defn inflate
  "Inflate the entity with current `state`."
  [state data-source options-map]
  (-> state

      ; include the data source in case we need it
      (assoc :wish/data-source data-source)

      (apply-levels data-source)
      (apply-options data-source options-map)))
