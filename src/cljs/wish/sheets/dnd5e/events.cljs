(ns ^{:author "Daniel Leong"
      :doc "dnd5e-specific events"}
  wish.sheets.dnd5e.events
  (:require [re-frame.core :refer [dispatch reg-event-db reg-event-fx
                                   trim-v]]
            [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
            [wish.sheets.dnd5e.util :refer [->slot-kw]]
            [wish.sheets.util :refer [update-uses]]
            [wish.util :refer [process-map]]))

(defn with-range
  [old-val f min-val max-val & args]
  (let [new-val (apply f old-val args)]
    (min
      max-val
      (max min-val new-val))))

(reg-event-fx
  ::update-hp
  [trim-v]
  (fn-traced [cofx _]
    (update-uses cofx :hp#uses inc)))

(reg-event-fx
  ::use-spell-slot
  [trim-v]
  (fn-traced [cofx [kind level max-slots]]
    (update-uses cofx (->slot-kw kind level) with-range inc 0 max-slots)))

(reg-event-fx
  ::restore-spell-slot
  [trim-v]
  (fn-traced [cofx [kind level max-slots]]
    (update-uses cofx (->slot-kw kind level) with-range dec 0 max-slots)))
