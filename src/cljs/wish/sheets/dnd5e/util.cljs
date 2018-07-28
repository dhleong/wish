(ns ^{:author "Daniel Leong"
      :doc "util"}
  wish.sheets.dnd5e.util
  (:require-macros [wish.util.log :as log])
  (:require [clojure.string :as str]
            [wish.sheets.dnd5e.data :as data]
            [wish.sources.compiler.limited-use :refer [compile-limited-use]]
            [wish.sources.compiler.fun :refer [->callable]]))

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

; TODO it'd be better to do this once at the datasource level
(def compile-ac-source (memoize ->callable))
(defn- compile-ac-sources
  [entity]
  (cond
    (get-in entity [:attrs :5e/ac])
    (update-in entity [:attrs :5e/ac]
               (fn [ac-sources-map]
                 (reduce-kv
                   (fn [m k v]
                     (assoc m k (compile-ac-source v)))
                   {}
                   ac-sources-map)))

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

                       ; should look like {:fn (fn [level])}
                       (update-in m [k :fn] compile-speed-buff)))
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
  [entity data-source entity-kind]
  (-> entity
      install-spell-uses
      compile-ac-sources
      compile-speed-buffs
      compile-weapon-dice))
