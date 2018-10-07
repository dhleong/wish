(ns ^{:author "Daniel Leong"
      :doc "DataSource compiler"}
  wish.sources.compiler
  (:require-macros [wish.util.log :as log :refer [log]])
  (:require [clojure.data :refer [diff]]
            [wish.sources.compiler.entity :refer [compile-entity]]
            [wish.sources.compiler.entity-mod :refer [apply-entity-mod merge-mods]]
            [wish.sources.compiler.feature :refer [compile-feature inflate-features]]
            [wish.sources.compiler.fun :refer [->callable]]
            [wish.sources.compiler.limited-use :refer [compile-limited-use]]
            [wish.sources.compiler.lists :refer [add-to-list inflate-items install-deferred-lists]]
            [wish.sources.compiler.race :refer [declare-race declare-subrace install-deferred-subraces]]
            [wish.sources.compiler.util :as util]
            [wish.sources.core :refer [find-feature]]
            [wish.util :refer [deep-merge inc-or ->map process-map]]))

; ======= constants ========================================

; wish compat version number for new character sheets
(def compiler-version 1)

; ======= options ==========================================

(defn compile-option
  [o]
  (update (if (contains? o :!)
            (compile-feature o)
            o)
          :available?
          ->callable))

; ======= directives =======================================

(declare apply-feature-directives)

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
   declare-race

   :!declare-subrace
   declare-subrace

   :!declare-items
   (fn declare-items [state base & item-maps]
     (update state :items
             merge
             (->> item-maps
                  (map (partial merge-with
                                (fn [a b]
                                  (cond
                                    (map? a)
                                    (merge a b)

                                    (coll? a)
                                    (concat a b)

                                    :else b))
                                base))
                  ->map)))

   ; NOTE: this should never be applied top-level,
   ; but only to specific classes, races, etc.
   :!provide-attr
   (fn provide-attr [state id value]
     (let [path (if (keyword? id)
                  [:attrs id]
                  (cons :attrs id))]
       (assoc-in state path value)))

   :!update-attr
   (fn update-attr [state path fn-symbol & value]
     (let [f (case fn-symbol
               ; NOTE: the case should pick up the left side as compile-time
               ; symbol constants, and the right should evaluate to the function
               + +
               - -
               / /
               * *
               inc inc
               dec dec
               (throw (js/Error. "Unexpected update-attr symbol:" fn-symbol)))]
       (apply update-in state (cons :attrs path) f value)))

   :!provide-feature
   (fn provide-feature [state & args]
     (loop [state state
            args args]
       (let [providing-feature (first (:wish/context state))
             base-sort-key (or (:wish/sort providing-feature)
                               (when-let [sorts (:wish/sorts providing-feature)]
                                 ; shouldn't happen
                                 (log/warn "Pick correct :sort for instanced feature" args)
                                 (first sorts)))

             raw-values-features (->> args
                                      (map :values)
                                      flatten
                                      (filter :id)
                                      (map #(assoc % :wish/option? true)))
             features (->> (inflate-features state
                                             (concat args
                                                     raw-values-features))
                           (map-indexed (fn [i f]
                                          (if base-sort-key
                                            ; NOTE: (inc) so that, when the base is padded,
                                            ; this definitely comes after base
                                            (assoc f :wish/sort (conj base-sort-key (inc i)))
                                            f))))

             features-map (->map features)
             features-with-directives (when (:wish/data-source state)
                                        (->> features
                                             (filter :!)
                                             seq))

             ; TODO apply filters?
             filters (filter vector? args)

             ; install new features always
             state (update state :features
                           (partial merge-with
                                    (fn [a b]
                                      (if a
                                        (-> a
                                            (update :wish/instances
                                                    ; a existing means there's at least 1
                                                    ; instance, so now there are at least 2
                                                    inc-or 2)

                                            ; ensure :wish/sorts exists with :wish/sort
                                            (util/combine-sorts b))

                                        ; just use the newer
                                        b)))
                           features-map)]

         (if features-with-directives
           ; apply directives for newly added features...
           (let [new-state (reduce
                             apply-feature-directives
                             state
                             features-with-directives)
                 [_ new-features _] (diff (:features state)
                                          (:features new-state))]
             ; ... then recursively apply features added by the application
             ; of features-with-directives
             (recur new-state (vals new-features)))

           ; done!
           state))))

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
(defn install-features
  ([s entity]
   (install-features s entity nil))
  ([s entity data-source]
   (-> entity
       (update :features
               (fn [features]
                 (reduce-kv
                   (fn [m feature-id v]
                     ; if the value is just a map indicating that this
                     ; is a secondary instance of the feature,
                     ; just load the feature as normal (2nd branch)
                     (if (and (:id v)
                              (not (:wish/instances v)))
                       ; ensure it's compiled
                       (assoc m feature-id (compile-feature v))

                       ; pull it out of the state (or data source, if we have one)
                       (let [from-state (get-in s [:features feature-id])

                             ; this is a bit obnoxious to avoid eager evaluation
                             ; from-state could be a number here
                             f (if (:id from-state)
                                 ; already inflated
                                 from-state

                                 ; inflate the feature
                                 (when data-source
                                   (find-feature data-source feature-id)))

                             ; include :wish/instances and :wish/sort
                             f (when f
                                 (merge v f))]
                         (assoc m feature-id f))))
                   features
                   features)))

       ; apply features
       (as-> e
         (->> e
              :features
              vals
              (mapcat (fn [{instances :wish/instances
                            directives :!
                            :as providing-feature}]
                        (->> (if instances
                               ; repeat the directives n times
                               (mapcat
                                 (constantly directives)
                                 (range instances))

                               directives)

                             ; attach context in meta to each directive
                             (map #(with-meta % {:wish/context providing-feature}))
                             )))
              (reduce apply-directive e))))))

; ======= public api =======================================

(defn apply-directive [state directive-vector]
  (try
    (let [[kind & args] directive-vector]
      (if-let [f (get directives kind)]
        ; valid directive
        (let [?context (:wish/context (meta directive-vector))

              ; if we had a context, push it on the stack
              state (cond-> state
                      ?context (update :wish/context conj ?context))

              ; apply the directive
              new-state (apply f state args)]

          ; we we pushed a context, pop it before returning the new state
          (cond-> new-state
            ?context (update :wish/context pop)))

        ; unknown; ignore
        (do
          (log/warn "unknown directive:" kind)
          state)))
    (catch js/Error e
      (throw (js/Error.
               (str "Error processing " directive-vector
                    "\n\nOriginal error: " (.-stack e) "\nThrown at:"))))))

(defn compile-directives
  "Given a sequence of directives, return a compiled DataSource state"
  [directives]
  (->> directives

       ; compile directives into a state
       (reduce apply-directive {})

       ; deferred processing, now that all directives have been applied
       ; and all features/options should be available
       install-deferred-lists
       install-deferred-options
       install-deferred-subraces

       (process-map :classes install-features)
       (process-map :races install-features)
       (process-map :lists inflate-items)

       ))


; ======= Options ==========================================

(defn- apply-feature-directives
  [state the-feature & {:keys [option-value providing-feature]}]
  ; only try to apply the feature if state's primary?-ness
  ; matches the feature's requirements
  (let [feature-directives (when-not (and (:primary-only? the-feature)
                                          (not (:primary? state)))
                             (:! the-feature))]
    (if feature-directives
      (-> state
          ; push the feature on the context stack
          (update :wish/context conj providing-feature)

          ; apply features to state with context attached
          (as-> s
            (reduce apply-directive s feature-directives))

          ; pop feature off the context stack
          (update :wish/context pop))

      (do
        ; NOTE: at this point, there may just be no directives to apply?
        (when-not the-feature
          (log/warn "failed to apply "
                    (when option-value
                      (str " from " option-value
                           " for feature " (:id providing-feature)))))
        state))))

(declare apply-levels)
(declare find-feature-scaling)
(defn- level-scale-feature
  [data-source level f]
  (if (map? f)
    (-> f
        (assoc :level level)
        (apply-levels
          data-source
          find-feature-scaling)
        (dissoc :level))

    ; unable
    f))

(defn- apply-feature-options
  [data-source state feature-id options-chosen]
  (let [instance-n (-> options-chosen meta :wish/instance)
        providing-feature (get-in state [:features feature-id])
        providing-feature (if (and instance-n
                                   (:wish/sorts providing-feature))
                            ; choose the right :wish/sort to use
                            (-> providing-feature
                                (dissoc :wish/sorts)
                                (assoc :wish/sort (-> (:wish/sorts providing-feature)
                                                      (reverse)
                                                      (nth instance-n nil))))
                            providing-feature)]

    (if (or (empty? options-chosen)

            ; don't apply any options if the feature doesn't apply to this state
            ; (EX: racial feature options on the class, or vice versa)
            (not providing-feature)

            ; also, if the providing feature is instanced, don't apply we don't have
            ; enough instances available (IE: they leveled up, selected, then leveled down)
            (when (:instanced? providing-feature)
              ; NOTE: :wish/instances can be nil if it's a single instance of the
              ; feature, but we know it exists with *at least* one because (:instanced?)
              ; is true
              (>= instance-n (:wish/instances providing-feature 1))))
      state

      (let [option-value (first options-chosen)
            the-feature (when-let [f (merge
                                       (when data-source
                                         (find-feature data-source option-value))
                                       (or (get-in state [:features option-value])
                                           (->> (get-in state [:features feature-id :values])
                                                (filter #(= option-value (:id %)))
                                                first)))]
                          ; level-scale the feature
                          (level-scale-feature data-source (:level state) f))]

        (recur
          data-source

          ; new state:
          (apply-feature-directives state the-feature
                                    :option-value option-value
                                    :providing-feature providing-feature)

          feature-id
          (next options-chosen))))))

(defn- instance-id->n
  "Given an instance ID, return the instance number/index"
  [id]
  (let [s (name id)
        sep-idx (.lastIndexOf s "#")]
    (if (= -1 sep-idx)
      -1
      (int (subs s (inc sep-idx))))))

(defn unpack-option
  "Given an entry in the :options map (eg: [feature-id v])
   unpack it to another vector, in case the `v` was a map
   for a multi-instance feature"
  [[inst-id v :as option-entry]]
  (if (map? v)
    [(:id v) (with-meta
               (:value v)
               {:wish/instance (instance-id->n inst-id)})]
    option-entry))

(defn apply-options
  [state data-source options-map]
  (if (empty? options-map)
    ; done!
    state

    ; we have to basically use options-map like a work queue, where if the
    ; feature-id doesn't exist *yet*, we continue applying other options in case
    ; they trigger more features to be provided that the option can later apply to
    (let [[applyable not-applyable] (reduce
                                      (fn [[a b] option-entry]
                                        (let [[feature-id _ :as option] (unpack-option
                                                                          option-entry)]
                                          (if (get-in state [:features feature-id])
                                            [(conj a option) b]
                                            [a (conj b option)])))
                                      [[] []]
                                      options-map)]
      (if (empty? applyable)
        ; nothing else to be done
        (do
          ; NOTE it's okay if some options aren't applied, like spellcaster lists, or if
          ; the feature is for a different class/race, so we don't warn by default. But
          ; this could be useful to uncomment for debugging issues
          ;; (when (seq not-applyable)
          ;;   (log/warn "Unable to apply " (map first not-applyable) " to " (:id state)))
          state)

        (recur
          ; NOTE apply all currently applyable at once to avoid extra (reduce) steps above
          (reduce
            (fn [state [feature-id options-chosen]]
              (apply-feature-options data-source state feature-id options-chosen))
            state
            applyable)

          data-source
          not-applyable)))))


; ======= Level-scaling ====================================

(defn- get-scaling
  [context k path]
  (when-let [scaling (k context)]
    [scaling path]))

(defn find-feature-scaling
  [state k]
  (let [features (keep
                   (fn [[id feature]]
                     (get-scaling feature k [:features id]))
                   (:features state))]
    (if-let [top-level (get-scaling state k nil)]
      (cons top-level features)
      features)))

(defn find-limited-use-scaling
  [state k]
  (keep
    (fn [[id limited-use]]
      (get-scaling limited-use k [:limited-uses id]))
    (:limited-uses state)))

(defn apply-mod-in
  "Apply mod-map in `path` and install newly-added features"
  [state data-source mod-map path]
  (let [after (if path
                (update-in state path apply-entity-mod mod-map)
                (apply-entity-mod state mod-map))
        ; only install features added
        [_ added _] (diff state after)

        level-scaler (partial level-scale-feature
                              (:wish/data-source state)
                              (:level state))

        new-installed (when (:features added)
                        (install-features
                          state
                          ; NOTE: copy attrs from the state to handle
                          ; incremental :attrs (like from :!update-attrs)
                          (-> after
                              (assoc :attrs
                                     (:attrs state))
                              (update :features
                                      (fn [m]
                                        (reduce-kv
                                          (fn [m f-id f]
                                            ; NOTE: if (= f-id (last path)),
                                            ; this is exactly the feature
                                            ; we just applied mods to
                                            (if (and (not= f-id (last path))
                                                     (map? f))
                                              ; level-scale, but not :!; that is
                                              ; handled elsewhere.
                                              ; FIXME This is a terrible hack, but otherwise
                                              ; top-level features added by level-scaling
                                              ; don't get any level scaling themselves...
                                              ; (See: Cleric's Destroy Undead)
                                              ; Hopefully fresh eyes can eventually
                                              ; figure out a better way to handle this
                                              (assoc m f-id
                                                     (-> f
                                                         level-scaler
                                                         (assoc :! (:! f))))

                                              ; no change
                                              m))
                                          m
                                          m))))
                          data-source))]
    (deep-merge after new-installed)))

(defn- apply-scaling-for-level
  [state path this-scaling data-source level]
  (if-let [values (get this-scaling level)]
    (apply-mod-in
      state data-source
      values path)

    ; no scaling; pass through
    state))

(defn- apply-levels-with
  [original-state data-source levels-key find-scaling-fn apply-fn]
  (loop [state original-state
         level (:level state)
         merge-scaling (find-scaling-fn state levels-key)]
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
  [original-state data-source find-scaling-fn]
  (apply-levels-with
    original-state
    data-source
    :&levels
    find-scaling-fn
    (fn [state path this-scaling data-source level]
      ; combine all mod maps up to this level and
      ; apply *once* to avoid dup applications
      (let [merged-mod-map (->> (range 1 (inc level))
                                (keep this-scaling)
                                (apply merge-mods))]
        (apply-mod-in
          state data-source
          merged-mod-map path)))))

(defn- apply-current-level
  "Applies :levels statements in `original-state`"
  [original-state data-source find-scaling-fn]
  (apply-levels-with
    original-state
    data-source
    :levels
    find-scaling-fn
    apply-scaling-for-level))

(defn apply-levels
  [state data-source find-scaling-fn]
  (-> state
      (apply-current-level data-source find-scaling-fn)
      (apply-all-levels data-source find-scaling-fn)))


; ======= Public interface =================================

(defn inflate
  "Inflate the entity with current `state`."
  [state data-source options-map]
  (-> state

      ; include the data source for things that need it
      (assoc :wish/data-source data-source
             :wish/options options-map)

      ; apply levels first so all base features are available,
      ; then apply options to get option-provided features and
      ; apply all their directives
      (apply-levels data-source find-feature-scaling)
      (apply-options data-source options-map)

      ; apply levels to limited-use items, since they're
      ; all available now
      (apply-levels data-source find-limited-use-scaling)))

(defn apply-directives
  "Apply all :! directives on an entity to that entity"
  [entity data-source]
  (if-let [directives (:! entity)]
    (loop [entity (assoc entity :wish/data-source data-source)
           directives directives]
      (if-let [d (first directives)]
        (recur
          (apply-directive entity d)
          (next directives))

        ; done
        (dissoc entity :wish/data-source)))

    ; nothing to do
    entity))
