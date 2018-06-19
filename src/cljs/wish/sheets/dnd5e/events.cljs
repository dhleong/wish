(ns ^{:author "Daniel Leong"
      :doc "dnd5e-specific events"}
  wish.sheets.dnd5e.events
  (:require [re-frame.core :refer [dispatch reg-event-db reg-event-fx
                                   trim-v]]
            [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
            [wish.sheets.dnd5e.util :refer [level->slot-kw]]
            [wish.sheets.util :refer [update-uses]]
            [wish.util :refer [process-map]]))

(defn with-range
  [old-val f min-val max-val & args]
  (let [new-val (apply f old-val args)]
    (min
      max-val
      (max min-val new-val))))

(reg-event-db
  ::update-hp
  [trim-v]
  (fn-traced [db _]
    (update-uses db :hp#uses inc)))

(reg-event-db
  ::use-spell-slot
  [trim-v]
  (fn-traced [db [level max-slots]]
    (update-uses db (level->slot-kw level) with-range inc 0 max-slots)))

(reg-event-db
  ::restore-spell-slot
  [trim-v]
  (fn-traced [db [level max-slots]]
    (update-uses db (level->slot-kw level) with-range dec 0 max-slots)))
