(ns ^{:author "Daniel Leong"
      :doc "dnd5e-specific events"}
  wish.sheets.dnd5e.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx
                                   trim-v]]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [wish.subs-util :refer [active-sheet-id]]
            [wish.sheets.dnd5e.util :refer [->slot-kw with-range]]
            [wish.sheets.util :refer [update-sheet update-in-sheet
                                      update-sheet-path
                                      get-uses update-uses]]))


; ======= 5e-specific nav =================================

(defn- page-nav
  "Create the event-db fn that navigates with the given nav-key"
  [nav-key]
  (fn-traced [db [new-page]]
    (let [sheet-id (active-sheet-id db)]
      (assoc-in db [nav-key sheet-id] new-page))))

(reg-event-db
  ::page!
  [trim-v]
  (page-nav :5e/page))

(reg-event-db
  ::actions-page!
  [trim-v]
  (page-nav :5e/actions-page))


; ======= builder-specific =================================

(defn remove-class
  [classes class-info]
  (let [removed (dissoc classes (:id class-info))]
    (if (:primary? class-info)
      ; we removed the primary class; promote another class
      ; to take its place
      (if-let [other-id (->> removed keys first)]
        (assoc-in removed [other-id :primary?] true)

        ; no other classes
        removed)

      ; easy case
      removed)))

(reg-event-fx
  ::remove-class
  [trim-v]
  (fn-traced [cofx [class-info]]
    (update-sheet-path cofx [:classes] remove-class class-info)))


; ======= search/filter ===================================

(defn- reg-filter-event [k]
  (reg-event-db
    k
    [trim-v]
    (fn-traced [db [filter-str]]
      (assoc db k filter-str))))

(reg-filter-event :5e/effects-filter)
(reg-filter-event :5e/items-filter)
(reg-filter-event :5e/spells-filter)


; ======= etc ==============================================

(reg-event-fx
  ::temp-hp!
  [trim-v]
  (fn-traced [cofx [amount]]
    (update-sheet cofx assoc :temp-hp amount)))

(reg-event-fx
  ::temp-max-hp!
  [trim-v]
  (fn-traced [cofx [amount]]
    (update-sheet cofx assoc :temp-max-hp amount)))

(defn update-hp
  [cofx amount max-hp]
  (let [{:keys [db]} cofx
        sheet-id (active-sheet-id db)
        temp-hp (get-in db [:sheets sheet-id :sheet :temp-hp])
        used-temp (when (< amount 0)
                    (if (> temp-hp 0)
                      (min temp-hp (Math/abs amount))))

        ; first, if we used any temp hp, use it
        cofx (if used-temp
               (update-in-sheet cofx [:temp-hp] - used-temp)
               cofx)

        ; update amount for used-temp, if any
        amount (if used-temp
                 ; if we have any used temp, amount is definitely negative
                 ; and used-temp will be positive; add together to reduce
                 ; the amount to use
                 (+ amount used-temp)

                 ; didn't use any; don't change amount
                 amount)

        ; if there's any amount left to apply, do it
        cofx (if (not= amount 0)
               ; NOTE: adjusting HP *uses* here, not *current* HP. So, for
               ; `[::update-hp 2]`, which should increase *current* hp,
               ; we need to reduce uses.
               (update-uses cofx :hp#uses with-range [0 max-hp] - amount)

               ; no change!
               cofx)]
    (if (> amount 0)
      ; we healed, so go ahead and reset death save use
      (assoc cofx :dispatch [::reset-death-saves])

      ; nothing to do
      cofx)))

(reg-event-fx
  ::update-hp
  [trim-v]
  (fn-traced [cofx [amount max-hp]]
    (update-hp cofx amount max-hp)))

(reg-event-fx
  ::use-spell-slot
  [trim-v]
  (fn-traced [cofx [kind level max-slots]]
    ; TODO use ::inject/sub not have to supply max-slots
    (update-uses cofx (->slot-kw kind level) with-range [0 max-slots] inc)))

(defn- build-effect-add-event [effect]
  (cond
    (keyword? effect) [:effect/add effect]
    (vector? effect) (into [:effect/add] effect)))

(reg-event-fx
  ::+use
  [trim-v]
  (fn-traced [cofx [info]]
    (if (not= :*spell-slot (:id info))
      ; easy case
      {:dispatch-n (list [:+use (:id info) 1]
                         (when-let [effect (:adds-effect info)]
                           (build-effect-add-event effect)))}

      ; special case for spells
      {:dispatch [::use-spell-slot
                  (:slot-kind info)
                  (:slot-level info)
                  (:max-slots info)]})))

(reg-event-fx
  ::toggle-used
  [trim-v]
  (fn-traced [cofx [info]]
    {:dispatch-n (list [:toggle-used (:id info)]
                       (when-let [effect (:adds-effect info)]
                         (when (= (get-uses cofx (:id info)) 0)
                           (build-effect-add-event effect))))}))

(reg-event-fx
  ::restore-spell-slot
  [trim-v]
  (fn-traced [cofx [kind level max-slots]]
    (update-uses cofx (->slot-kw kind level) with-range [0 max-slots] dec)))

(reg-event-fx
  ::adjust-currency
  [trim-v]
  (fn-traced [cofx [adjust-map]]
    (update-in-sheet cofx [:currency] (partial merge-with +) adjust-map)))

(reg-event-fx
  ::set-currency
  [trim-v]
  (fn-traced [cofx [currency-id amount]]
    (update-in-sheet cofx [:currency] assoc currency-id amount)))

(reg-event-fx
  ::set-notes
  [trim-v]
  (fn-traced [cofx [new-notes]]
    (update-sheet cofx assoc :notes new-notes)))

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

; expects eg [inc :saves] or [dec :fails]
(reg-event-fx
  ::update-death-saves
  [trim-v]
  (fn-traced [cofx [m kind]]
    (update-in-sheet cofx [:death-saving-throws kind]
                     with-range [0 3]
                     m)))

(reg-event-fx
  ::reset-death-saves
  [trim-v]
  (fn-traced [cofx [kind m]]
    (update-sheet cofx dissoc :death-saving-throws)))

(reg-event-fx
  ::toggle-attuned
  [trim-v]
  (fn-traced [cofx [item]]
    (update-in-sheet cofx [:attuned]
                     (fn [attuned]
                       (let [attuned-set (if (set? attuned)
                                           attuned
                                           (set attuned))
                             item-id (:id item)]
                         (if (contains? attuned-set
                                        item-id)
                           (disj attuned-set item-id)
                           (conj attuned-set item-id)))))))
