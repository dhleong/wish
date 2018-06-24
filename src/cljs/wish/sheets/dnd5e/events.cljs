(ns ^{:author "Daniel Leong"
      :doc "dnd5e-specific events"}
  wish.sheets.dnd5e.events
  (:require [re-frame.core :refer [dispatch reg-event-db reg-event-fx
                                   trim-v]]
            [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
            [wish.sheets.dnd5e.util :refer [->slot-kw]]
            [wish.sheets.util :refer [update-in-sheet update-uses]]
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

(defn update-hp-rolled
  [hp-rolled-map [class-id level-idx :as path] rolled]
  (if (vector? (get hp-rolled-map class-id))
    ; easy case
    (assoc-in hp-rolled-map path rolled)

    ; key doesn't exist yet or isn't a vector; create it
    (assoc hp-rolled-map class-id
           (assoc
             (vec
               (repeat (inc level-idx)
                       nil))
             level-idx
             rolled))))

; set rolled hp amount for [`class-id` `level-1`]
(reg-event-fx
  ::set-rolled-hp
  [trim-v]
  (fn-traced [cofx [path v]]
    (update-in-sheet cofx [:hp-rolled] update-hp-rolled path v)))
