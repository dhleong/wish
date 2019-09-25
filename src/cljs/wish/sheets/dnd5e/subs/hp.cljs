(ns wish.sheets.dnd5e.subs.hp
  (:require [re-frame.core :as rf :refer [reg-sub]]
            [wish.sheets.dnd5e.util :as util :refer [ability->mod ->die-use-kw]]
            [wish.sheets.dnd5e.subs.abilities :as abilities]
            [wish.sheets.dnd5e.subs.util :refer [reg-sheet-sub]]
            [wish.subs-util :refer [reg-id-sub query-vec->preferred-id]]
            [wish.util :refer [<sub]]))

(reg-sheet-sub
  ::death-saving-throws
  :death-saving-throws)

(reg-sheet-sub
  ::temp
  :temp-hp)

(reg-sheet-sub
  ::temp-max
  :temp-max-hp)

(reg-id-sub
  ::rolled
  :<- [:meta/sheet]
  (fn [sheet [_ ?path]]
    (get-in sheet (concat
                    [:hp-rolled]

                    ; NOTE: as an id-sub, we can also be called
                    ; where the var at this position is the sheet id
                    (when (coll? ?path)
                      ?path)))))

(reg-id-sub
  ::max-hp-mode
  :<- [:meta/sheet]
  (fn [sheet]
    (or (:max-hp-mode sheet)

        ; if not specified and they have any rolled, use that
        (when (:hp-rolled sheet)
          :manual)

        ; default to :average for new users
        :average)))

(reg-id-sub
  ::rolled-max
  :<- [::rolled]
  :<- [:wish.sheets.dnd5e.subs/class->level]
  (fn [[rolled-hp class->level]]
    (->> rolled-hp

         ; if you set a class to level 3, set HP, then go back
         ; to level 2, there will be an orphaned entry in the
         ; rolled-hp vector. We could remove that entry when
         ; changing the level, but accounting for it here means
         ; that an accidental level-down doesn't lose your data
         ; Also, if you've removed a class that you once rolled HP
         ; for, we don't care about that class's old, rolled hp
         (reduce-kv
           (fn [all-entries class-id class-rolled-hp]
             (if-let [class-level (class->level class-id)]
               (concat all-entries
                       (take class-level
                             class-rolled-hp))

               ; no change
               all-entries))
           nil)

         (apply +))))

(reg-id-sub
  ::average-max
  :<- [:classes]
  (fn [classes]
    (reduce
      (fn [total c]
        (let [{:keys [primary? level]
               {hit-die :5e/hit-dice} :attrs} c

              ; the primary class gets full HP at first level,
              ; so remove one from it (we add this special case below)
              level (if primary?
                      (dec level)
                      level)]
          (+ total

             (when primary?
               hit-die)

             (* level
                (inc (/ hit-die 2))))))

      0 ; start at 0
      classes)))

(reg-id-sub
  ::max
  (fn [query-vec]
    [; NOTE: this <sub is kinda gross but I *think* it's okay?
     ; subscriptions are de-dup'd so...?
     ; The only other way would be to always subscribe to both,
     ; and that seems worse
     (case (<sub [::max-hp-mode (query-vec->preferred-id query-vec)])
       :manual [::rolled-max]
       :average [::average-max])

     [::temp-max]
     [::abilities/all]
     [:total-level]
     [:wish.sheets.dnd5e.subs/buffs :hp-max]])
  (fn [[base-max temp-max abilities total-level buffs]]
    (+ base-max

       temp-max

       (* total-level
          (->> abilities
               :con
               ability->mod))

       buffs)))

(reg-id-sub
  ::state
  :<- [::temp]
  :<- [::max]
  :<- [:limited-used]
  (fn [[temp-hp max-hp limited-used-map]]
    (let [used-hp (or (:hp#uses limited-used-map)
                      0)]
      [(+ temp-hp
          (- max-hp used-hp)) max-hp])))

; returns a list of {:die,:classes,:used,:total}
; where :classes is a list of class names, sorted by die size.
(reg-sub
  ::hit-dice
  :<- [:classes]
  :<- [:limited-used]
  (fn [[classes used]]
    (->> classes
         (reduce
           (fn [m c]
             (let [die-size (-> c :attrs :5e/hit-dice)]
               (if (get m die-size)
                 ; just add our class and inc the total
                 (-> m
                     (update-in [die-size :classes] conj (:name c))
                     (update-in [die-size :total] + (:level c)))

                 ; create the initial spec
                 (assoc m die-size {:classes [(:name c)]
                                    :die die-size
                                    :used (get used (->die-use-kw die-size))
                                    :total (:level c)}))))
           {})
         vals
         (sort-by :die #(compare %2 %1)))))

