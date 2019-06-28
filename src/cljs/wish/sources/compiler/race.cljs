(ns ^{:author "Daniel Leong"
      :doc "race"}
  wish.sources.compiler.race
  (:require-macros [wish.util.log :as log])
  (:require [wish.sources.compiler.entity :refer [compile-entity]]
            [wish.sources.compiler.entity-mod :refer [apply-entity-mod]]))

(defn- install-subrace
  [state parent-race-id subrace-map]
  (when-let [parent (get-in state [:races parent-race-id])]
    (update state :races
            assoc
            (:id subrace-map)
            (assoc (apply-entity-mod parent subrace-map)
                   :subrace-of parent-race-id))))

(defn declare-race [state race-map]
  (update state :races
          assoc
          (:id race-map) (compile-entity race-map)))

(defn declare-subrace [state parent-race-id subrace-map]
  (if-let [s (install-subrace state parent-race-id subrace-map)]
    ; installed!
    s

    ; couldn't find parent; may be in a different datasource?
    (update state :deferred-subraces
            assoc
            (:id subrace-map)
            (assoc subrace-map :subrace-of parent-race-id))))

(defn install-deferred-subraces [s]
  (let [races (:deferred-subraces s)
        s (reduce-kv

            (fn [state _subrace-id subrace-map]
              (let [parent-race-id (:subrace-of subrace-map)]
                (if-let [installed-state (install-subrace state parent-race-id subrace-map)]
                  (update installed-state :deferred-subraces dissoc)

                  ; still no? okay that's no good. It could still be in another data source, though!
                  (do
                    (log/warn "Parent race " parent-race-id " for " (:id subrace-map) " not found")
                    state))))

            s
            races)]
    (if (empty? (:deferred-subraces s))
      (dissoc s :deferred-subraces)

      ; keep it around
      s)))

