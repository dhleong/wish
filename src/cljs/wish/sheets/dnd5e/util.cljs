(ns ^{:author "Daniel Leong"
      :doc "util"}
  wish.sheets.dnd5e.util
  (:require [wish.sources.compiler.limited-use :refer [compile-limited-use]]
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
    (str "+" modifier)
    (str "−" (Math/abs modifier))))


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
  (update-in entity [:attrs :5e/ac]
             (fn [ac-sources-map]
               (reduce-kv
                 (fn [m k v]
                   (assoc m k (compile-ac-source v)))
                 {}
                 ac-sources-map))))

(defn post-process
  [entity data-source entity-kind]
  (-> entity
      install-spell-uses
      compile-ac-sources))
