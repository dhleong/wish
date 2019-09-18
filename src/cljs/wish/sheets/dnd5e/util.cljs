(ns ^{:author "Daniel Leong"
      :doc "util"}
  wish.sheets.dnd5e.util
  (:require [clojure.string :as str]
            [wish.sheets.dnd5e.data :as data]
            [wish.sources.core :as src]
            [wish.sources.compiler.limited-use :refer [compile-limited-use]]
            [wish.sources.compiler.fun :refer [->callable]]
            [wish.util :refer [update-each-value]]))

; ======= Shared utils =====================================

(defn ability->mod
  [score]
  (Math/floor (/ (- score 10) 2)))

(defn ->slot-kw
  ([level]
   (->slot-kw :standard level))
  ([kind level]
   (keyword (if (= kind :standard)
              "slots"
              (name kind))
            (str "level-" level))))

(defn ->die-use-kw
  [die-size]
  (keyword "hit-dice"
           (str "d" die-size "#uses")))

(defn mod->str
  [modifier]
  (if (>= modifier 0)
    (str "+" (or modifier 0))
    (str "−" (Math/abs modifier))))

(defn with-range
  "To be used as, eg:
     (update m :key with-range [min-val max-val] inc)"
  [old-val [min-val max-val] f & args]
  (let [new-val (apply f old-val args)]
    (min
      max-val
      (max min-val new-val))))


; ======= item-related =====================================

(def ^:private equippable-types
  #{:weapon :armor :gear})

(defn equippable?
  [item]
  (-> item :type equippable-types))


; ======= spell-related ====================================

(defn bonus-action?
  "Check if the given spell is cast as a bonus action"
  [s]
  (str/includes? (:time s)
                 "onus"))  ; avoid capitalization inconsistency

(defn reaction?
  "Check if the given spell is cast as a reaction"
  [s]
  (str/includes? (:time s)
                 "eaction"))  ; avoid capitalization inconsistency



; ======= :attr application ================================

(defn spellcasting-uses
  [slots-type restore-trigger]
  (reduce
    (fn [m level]
      (let [id (->slot-kw slots-type level)]
        (assoc m id
               (compile-limited-use
                 {:id id
                  :implicit? true
                  :restore-trigger restore-trigger}))))
    {}
    (range 1 10)))

(defn- install-spell-uses
  [entity]
  (let [spellcaster (-> entity :attrs :5e/spellcaster)
        slots-type (:slots-type spellcaster :standard)
        restore-trigger (:restore-trigger spellcaster :long-rest)
        basic-slot-id (->slot-kw slots-type 1)
        existing-slot-use (-> entity :limited-uses basic-slot-id)]
    (cond
      ; some kind of spellcasting
      (and spellcaster
           (not existing-slot-use))
      (update entity :limited-uses merge (spellcasting-uses
                                           slots-type
                                           restore-trigger))

      ; no spellcasting
      :else entity)))

;; NOTE some classes provide this as a feature, so we can't
;; do this as part of the post-compile step
(def compile-ac-source (memoize ->callable))
(defn- compile-ac-sources
  [entity]
  (cond
    (get-in entity [:attrs :5e/ac])
    (update-in entity [:attrs :5e/ac]
               update-each-value
               compile-ac-source)

    ; armor AC, etc. based on a builtin type
    (= :armor (:type entity))
    (data/inflate-armor entity)

    ; nope
    :else entity))

(def compile-speed-buff (memoize ->callable))
(defn- compile-speed-buffs
  [entity]
  (if (get-in entity [:attrs :buffs :speed])
    (update-in entity [:attrs :buffs :speed]
               (fn [buffs-map]
                 (reduce-kv
                   (fn [m k v]
                     (if (number? v)
                       m

                       ; should look like (fn [level])
                       (update m k compile-speed-buff)))
                   buffs-map
                   buffs-map)))

    ; nope
    entity))

(defn- compile-weapon-dice
  [entity]
  (if (= :weapon (:type entity))
    (data/inflate-weapon entity)

    ; not a weapon
    entity))

(defn post-process
  [entity _data-source _entity-kind]
  (-> entity
      install-spell-uses
      compile-ac-sources
      compile-speed-buffs
      compile-weapon-dice))


; ======= post-compile ====================================

(defn- compile-multiclass-and [reqs]
  (fn [abilities]
    (some
      (fn [[stat min-value]]
        (let [v (get abilities stat 1)]
          (when (< v min-value)
            (str (str/upper-case
                   (name stat))
                 " " min-value
                 " (is: " v ")"))))
      reqs)))

(defn compile-multiclass-reqs
  "Turns a multiclass reqs spec into an acceptor function.
   Reqs may either be a map, to indicate that ALL such
   stats must match, or a list (`()`) of such maps, to indicate
   that ONE OF of the maps must match."
  [reqs]
  (if (list? reqs)
    (let [any-of (map compile-multiclass-and reqs)]
      (fn [sheet]
        (let [failures (map (fn [f]
                              (f sheet))
                            any-of)]
          (when-not (some nil? failures)
            (->> failures
                 (str/join ", or ")
                 (str "None of: "))))))

    (compile-multiclass-and reqs)))

(defn post-compile-class [c]
  (-> c
      (update-in [:attrs :5e/multiclass-reqs]
                 compile-multiclass-reqs)))

(defn post-compile [data]
  (-> data
      (update :classes update-each-value post-compile-class)))
