(ns ^{:author "Daniel Leong"
      :doc "util"}
  wish.sheets.dnd5e.util
  (:require [wish.sources.compiler.limited-use :refer [compile-limited-use]]))

; ======= Shared utils =====================================

(defn ability->mod
  [score]
  (Math/floor (/ (- score 10) 2)))

(defn level->slot-kw
  [level]
  (keyword "slots" (str "level-" level)))

(defn mod->str
  [modifier]
  (if (>= modifier 0)
    (str "+" modifier)
    (str "−" (Math/abs modifier))))


; ======= :attr application ================================

(def spellcasting-uses
  (reduce
    (fn [m level]
      (let [id (level->slot-kw level)]
        (assoc m id
               (compile-limited-use
                 {:id id
                  :implicit? true
                  :restore-trigger :long-rest}))))
    {}
    (range 1 10)))

(defn- install-spell-uses
  [entity]
  (let [spellcaster (-> entity :attrs :5e/spellcaster)
        existing-uses (-> entity :limited-uses :slots/level-1)]
    (cond
      ; spellcaster with normal restore
      (and spellcaster
           (not existing-uses)
           (= :long-rest (:restore-trigger
                           spellcaster
                           :long-rest)))
      (update entity :limited-uses merge spellcasting-uses)

      ; TODO classes like warlock have a separate set of
      ; spell slots with a different restore trigger

      ; no spellcasting
      :else entity)))

(defn post-process
  [entity data-source entity-kind]
  (-> entity
      install-spell-uses))
