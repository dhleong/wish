(ns ^{:author "Daniel Leong"
      :doc "dnd5e.subs"}
  wish.sheets.dnd5e.subs
  (:require [re-frame.core :as rf :refer [reg-sub subscribe]]
            [wish.sheets.dnd5e.data :as data]
            [wish.sheets.dnd5e.subs.abilities :as abilities]
            [wish.sheets.dnd5e.subs.inventory :as inventory]
            [wish.sheets.dnd5e.subs.proficiency :as proficiency]
            [wish.sheets.dnd5e.subs.spells :as spells]
            [wish.sheets.dnd5e.subs.util :refer [compute-buffs reg-sheet-sub]]
            [wish.subs-util :refer [reg-id-sub]]
            [wish.util :refer [invoke-callable]]))


; ======= utility subs ====================================


; the ::buffs sub takes a single :buff type ID (not including an ability,
; since some buffs depend on ability modifiers) and computes and combines
; all attributed buffs across classes and races
(reg-id-sub
  ::buffs
  :<- [:effect-ids-set]
  :<- [::abilities/modifiers]
  :<- [:total-level]
  :<- [::base-speed]
  :<- [:races]
  :<- [:classes]
  :<- [::inventory/attuned]
  :<- [:effects]
  (fn [[effects-set modifiers total-level base-speed races & entity-lists]
       [_ & buff-path]]
    (let [full-buff-path (into [:attrs :buffs] buff-path)]
      (->> entity-lists

           ; NOTE some racial abilities buff based on the total class level
           (concat (map #(assoc % :level total-level) races))

           flatten

           (reduce
             (fn [^number total-buff entity]
               (+ total-buff
                  (let [buffs (get-in entity full-buff-path)]
                    (cond
                      (nil? buffs) 0
                      (number? buffs) buffs
                      (map? buffs) (compute-buffs
                                     (assoc entity
                                            ; hopefully there are few other things
                                            ; that can be doubled...
                                            :speed (+ base-speed
                                                      (when (= buff-path [:speed])
                                                        total-buff))
                                            :effects effects-set
                                            :modifiers modifiers)
                                     buffs)

                      :else (throw (js/Error.
                                     (str "Unexpected buffs value for "
                                          buff-path
                                          ": " (type buffs)
                                          " -> `" buffs "`")))))))
             0)))))

(reg-id-sub
  ::buff-attrs
  :<- [:all-attrs]
  (fn [attrs [_ buff-id]]
    (get-in attrs [:buffs buff-id])))



; ======= class and level ==================================

(reg-id-sub
  ::class->level
  :<- [:classes]
  (fn [classes _]
    (reduce
      (fn [m c]
        (assoc m (:id c) (:level c)))
      {}
      classes)))

(reg-id-sub
  ::class-level
  :<- [::class->level]
  (fn [classes [_ ?sheet-id ?class-id]]
    ; NOTE: when called normally, ?sheet-id is actually the class-id.
    ; when called as an id-sub, we use ?class-id
    (get classes (or ?class-id
                     ?sheet-id))))


; ======= limited use =====================================

(reg-id-sub
  ::limited-use-configs
  :<- [:all-limited-use-configs]
  :<- [:total-level]
  :<- [::abilities/modifiers]
  :<- [::inventory/attuned-ids]
  (fn [[items total-level modifiers attuned-set]]
    (->> items
         (remove :implicit?)

         ; eagerly evaluate :uses (the sheet shouldn't do this)
         (map (fn [limited-use]
                (update limited-use
                        :uses
                        (fn [value]
                          (if (ifn? value)
                            (invoke-callable limited-use :uses
                                             :modifiers modifiers
                                             :total-level total-level)
                            value)))))

         ; remove uses that come from un-attuned items that require attunement
         (remove (fn [item]
                   (and (= :item (:wish/context-type item))
                        (:attunes? (:wish/context item))
                        (not (contains? attuned-set (:id (:wish/context item)))))))

         (sort-by :name))))

(reg-sub
  ::limited-use
  :<- [::limited-use-configs]
  :<- [:limited-used]
  (fn [[items used] [_ id]]
    (->> items
         (filter #(= id (:id %)))
         (map #(assoc % :uses-left (- (:uses %)
                                      (get used id))))
         first)))

; Takes an entity with :consumes and returns something
; that can be consumed from it. Usually this delegates to
; [::limited-use (:consumes a)], but this also supports
; the special case of consuming a :*spell-slot
(reg-sub
  ::consumable
  (fn [[_ {id :consumes :as entity}]]
    (if (not= :*spell-slot id)
      (subscribe [::limited-use id])

      ; special case
      (subscribe [::spells/usable-slot-for entity])))
  (fn [input [_ {id :consumes}]]
    (if (not= :*spell-slot id)
      ; easy case
      input

      {:id :*spell-slot
       :name (str (get data/level-suffixed (:level input))
                  "-level Spell Slot")
       :uses-left (:unused input)
       :slot-kind (:kind input)
       :slot-level (:level input)
       :max-slots (:total input)}
      )))


; ======= general stats for header =========================

(reg-sub
  ::passive-perception
  :<- [::abilities/modifiers]
  :<- [::proficiency/bonus]
  :<- [::proficiency/saves]
  (fn [[abilities prof-bonus save-profs]]
    (+ 10
       (:wis abilities)
       (when (:wis save-profs)
         prof-bonus))))

(reg-sub
  ::base-speed
  :<- [:race]
  (fn [race]
    (-> race :attrs :5e/speed)))

(reg-sub
  ::speed
  :<- [::base-speed]
  :<- [::buffs :speed]
  (fn [[base buffs]]
    (+ base buffs)))


; ======= etc ==============================================

(reg-sheet-sub
  ::conditions
  :conditions)

(reg-sheet-sub
  ::notes
  :notes)

(reg-sub
  ::selected-option-ids
  :<- [:meta/options]
  (fn [options]
    (->> options
         vals
         flatten
         set)))

; hacks?
(reg-sub
  ::features-for
  (fn [[_ sub-vec]]
    [(subscribe sub-vec)
     (subscribe [:meta/options])
     (subscribe [::selected-option-ids])])
  (fn [[sources options selected-options]]
    (->> sources
         ; the sub-vec :class-features or :race-features
         ; returns a seq of map entries; we just want the values
         (map second)

         (filter :name)
         (remove :implicit?)

         (map (fn [f]
                (if-let [inst-id (:wish/instance-id f)]
                  (assoc f :id inst-id)
                  f)))

         ; if the feature is an option, we only want it
         ; if it was actually selected
         (remove #(and (:wish/option? %)
                       (not (contains? selected-options
                                       (:id %)))))

         ; filter out un-selected values
         (map (fn [f]
                (if-let [chosen (get options (:id f))]
                  (let [chosen (if (map? chosen)
                                 ; instanced feature
                                 (:value chosen)
                                 chosen)]
                    (update f :values
                            (partial filter
                                     #(some #{(:id %)} chosen))))

                  (dissoc f :values))))

         (sort-by :name)
         seq)))
