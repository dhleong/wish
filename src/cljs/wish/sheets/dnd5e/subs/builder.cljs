(ns wish.sheets.dnd5e.subs.builder
  (:require [re-frame.core :as rf :refer [reg-sub subscribe]]
            [wish.sheets.dnd5e.builder.data :refer [point-buy-max
                                                    score-point-cost]]
            [wish.sheets.dnd5e.util :as util]
            [wish.sheets.dnd5e.subs.abilities :as abilities]
            [wish.sheets.dnd5e.subs.spells :as spells]))

; ======= builder-specific =================================

(reg-sub
  ::abilities-mode
  :<- [:meta/sheet]
  (fn [sheet]
    (or
      ; normal case
      (:abilities-mode sheet)

      ; legacy compat
      (when-let [existing (->> sheet :abilities vals seq)]
        (when (not= #{8 10 12 13 14 15}
                    (set existing))
          :manual))

      ; default for new characters
      :standard)))

(defn calculate-scores-cost
  "Given a map of abilities, calculate how many points it costs"
  [abilities]
  (->> abilities
       vals
       (map score-point-cost)
       (apply +)))

; number of "points" remaining when using the point-buy system
; of ability score generation
(reg-sub
  ::point-buy-remaining
  :<- [::abilities/raw]
  (fn [abilities]
    (- point-buy-max
       (calculate-scores-cost abilities))))

(reg-sub
  ::point-buy-delta
  :<- [::abilities/raw]
  (fn [abilities [_ ability new-cost]]
    (let [current-cost (->> abilities
                            ability
                            (get score-point-cost))]
      (- current-cost
         new-cost))))

(defn- multiclass-error
  "Returns nil if the given class `c` can be multiclassed into,
   else a String explanation of the ability prereqs that weren't met"
  [c abilities]
  (when-let [get-error (get-in c [:attrs :5e/multiclass-reqs])]
    (get-error abilities)))

(defn- available-classes
  [all-classes selected-classes primary-class abilities]
  (let [first-class? (empty? selected-classes)
        ; there can't be a multiclass error for the first class
        primary-multiclass-error (when-not first-class?
                                   (multiclass-error
                                     primary-class
                                     abilities))
        selected-class-ids (->> selected-classes
                                (map :id)
                                (into #{}))]
    (->> all-classes
         (remove (comp selected-class-ids :id))
         (map
           (fn [c]
             (if
               ; if the primary can't multiclass,
               ; nobody can!
               primary-multiclass-error
               (assoc c :prereqs-failed? true
                      :prereqs-reason (str "Starting class does not meet multiclass prerequisites: "
                                           primary-multiclass-error))

               ; if primary is good, then this class's reqs must also be satisfied
               (if-let [err (when-not first-class?
                              (multiclass-error
                                c
                                abilities))]
                 (assoc c :prereqs-failed? true
                        :prereqs-reason (str "Multiclass prerequisites not met: "
                                             err))

                 ; good to go!
                 c))
             ))
         (sort-by :name))))

(reg-sub
  ::available-classes
  :<- [:available-entities :classes]
  :<- [:classes]
  :<- [::primary-class]
  :<- [::abilities/base]
  (fn [[all-classes selected-classes primary-class abilities]]
    (available-classes
      (map util/prepare-class-for-builder all-classes)
      selected-classes
      (util/prepare-class-for-builder primary-class)
      abilities)))

(reg-sub
  ::available-races
  :<- [:available-entities :races]
  (fn [all-races]
    (->> all-races
         (sort-by (fn [e]
                    [(or (:subrace-of e)
                         (:id e))
                     (:subrace-of e)
                     (:name e)])))))

(reg-sub
  ::primary-class
  :<- [:classes]
  (fn [classes]
    (->> classes
         (filter :primary?)
         first)))

(reg-sub
  ::class-by-id
  :<- [:classes]
  (fn [classes [_ id]]
    (->> classes
         (filter #(= id (:id %)))
         first)))

(reg-sub
  ::background
  :<- [:race-features-with-options]
  (fn [features]
    (->> features
         (filter #(= :background (first %))))))

(def ^:private custom-background-feature-ids
  #{:custom-bg/skill-proficiencies
    :custom-bg/feature
    :custom-bg/tools-or-languages})
(reg-sub
  ::custom-background
  :<- [:race-features-with-options]
  (fn [features]
    (->> features
         (filter #(custom-background-feature-ids (first %))))))

; like the default one, but removing :background
(reg-sub
  ::race-features-with-options
  :<- [:race-features-with-options]
  (fn [features]
    (->> features
         (remove #(= :background (first %)))
         (remove #(custom-background-feature-ids (first %))))))

; like the default one, but providing special handling for :all-spells
(reg-sub
  ::class-features-with-options
  (fn [[_ entity-id]]
    [(subscribe [:class-features-with-options entity-id])
     (subscribe [::spells/highest-spell-level-for-spellcaster-id entity-id])])
  (fn [[features highest-spell-level]]
    (->> features
         (map (fn [[id f :as entry]]
                (if (= [:all-spells]
                       (:wish/raw-values f))
                  ; when a feature lets you pick *any* spell, it's always
                  ; limited to spells you can actually cast
                  [id (update f :values
                              (partial filter
                                       #(<= (:spell-level %)
                                            highest-spell-level)))]

                  ; normal case
                  entry))))))

(reg-sub
  ::available-feature-options
  (fn [[_ source-sub _feature-id instance-id]]
    [(subscribe source-sub)
     (subscribe [:options-> (if (seq? instance-id)
                              instance-id
                              [instance-id])])])
  (fn [[features options] [_ _ feature-id _instance-id]]
    ; options are available either if we have it selected,
    ; or if it is explicitly available. This way, feature options
    ; that are shared across features but which should not be
    ; available more than once can mark themselves as unavailable
    ; when chosen, and still be seen in the character builder
    (->> features
         (filter #(= feature-id (first %)))
         first  ; first (only) match
         second ; the feature
         :values

         (filter :available?)
         (map :id)

         (concat options)
         (into #{}))))

; returns a bit more than just the hit die, for convenience
(reg-sub
  ::class->hit-die
  :<- [:classes]
  (fn [classes]
    (reduce
      (fn [m c]
        (assoc m (:id c)
               (assoc
                 (select-keys c [:name :level])
                 :dice (-> c :attrs :5e/hit-dice))))
      {}
      classes)))

