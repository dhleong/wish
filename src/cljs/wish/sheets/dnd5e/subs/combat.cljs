(ns wish.sheets.dnd5e.subs.combat
  (:require [clojure.string :as str]
            [re-frame.core :as rf :refer [reg-sub]]
            [wish-engine.core :as engine]
            [wish.sources.util :as src-util]
            [wish.sheets.dnd5e.data :as data]
            [wish.sheets.dnd5e.subs.abilities :as abilities]
            [wish.sheets.dnd5e.subs.inventory :as inventory]
            [wish.sheets.dnd5e.subs.proficiency :as proficiency]
            [wish.sheets.dnd5e.subs.util :refer [feature-by-id]]
            [wish.util :refer [invoke-callable]]))

(reg-sub
  ::armor-equipped?
  :<- [::inventory/equipped]
  (fn [equipped]
    (some data/armor? equipped)))

(reg-sub
  ::shield-equipped?
  :<- [::inventory/equipped]
  (fn [equipped]
    (some data/shield? equipped)))

(reg-sub
  ::ac
  :<- [:classes]
  :<- [:effects]
  :<- [::inventory/attuned]
  :<- [::abilities/modifiers]
  :<- [:wish.sheets.dnd5e.subs/buffs :ac]
  :<- [::armor-equipped?]
  :<- [::shield-equipped?]
  (fn [[classes effects equipped modifiers ac-buff armor? shield?]]
    (let [ac-sources (->> (concat classes
                                  effects
                                  equipped)
                          (mapcat (comp vals :5e/ac :attrs)))
          fn-context {:modifiers modifiers
                      :armor? armor?
                      :shield? shield?}]
      (+ ac-buff

         (apply max

                ; unarmored AC (not available if we have armor equipped)
                (+ 10
                   (:dex modifiers))

                (map #(% fn-context) ac-sources))))))

(reg-sub
  ::initiative
  :<- [::abilities/modifiers]
  :<- [::abilities/skill-half-proficiencies]
  :<- [::proficiency/bonus]
  :<- [:wish.sheets.dnd5e.subs/buffs :initiative]
  (fn [[abilities half-proficiencies prof-bonus buffs]]
    (+ (:dex abilities)

       (when (contains? half-proficiencies :initiative)
         (Math/floor (/ prof-bonus 2)))

       buffs)))

(reg-sub
  ::attacks-per-action
  :<- [:classes]
  (fn [classes _]
    (or (->> classes
             (map (juxt (comp vals :attacks-per-action :attrs)
                        identity))
             (mapcat (fn [[values entity]]
                       (map
                         (fn [v]
                           (if (number? v)
                             ; easy case
                             v

                             ; functional value
                             (v entity)))
                         values)))
             (apply max))
        1)))

(reg-sub
  ::info
  :<- [::attacks-per-action]
  :<- [:classes]
  (fn [[attacks-per-action classes] _]
    (->> classes
         (map (juxt (comp vals :combat-info :attrs)
                    identity))
         (mapcat (fn [[values entity]]
                   (map
                     (fn [v]
                       (update v :value
                               (fn [value]
                                 (value entity))))
                     values)))
         (sort-by :name)

         ; insert attacks
         (cons {:name "Attacks per action"
                :value attacks-per-action}))))

(reg-sub
  ::unarmed-strike
  :<- [:sheet-engine-state]
  :<- [::abilities/modifiers]
  :<- [::proficiency/bonus]
  :<- [:races]
  :<- [:classes]
  (fn [[data-source modifiers proficiency-bonus & feature-sources]]
    ; prefer the first non-implicit result
    (->> feature-sources
         (apply concat)
         (map (fn [c]
                (assoc
                  (or (-> c :features :unarmed-strike)
                      ; possibly a custom class without a custom :unarmed-strike;
                      ; fall back gracefully to the default
                      (get-in data-source [:features :unarmed-strike]))
                  :wish/context c
                  :wish/context-type :class)))

         ; sort-by is in ascending order
         (sort-by #(if (:implicit? %)
                     1
                     0))

         ; insert :to-hit calculation
         (map (fn [s]
                (assoc s
                       :to-hit (if (:finesse? s)
                                 (+ proficiency-bonus
                                    (max (:str modifiers)
                                         (:dex modifiers)))
                                 (+ proficiency-bonus (:str modifiers)))
                       :dmg (invoke-callable
                               s :dice
                               :modifiers modifiers))))

         first)))

(reg-sub
  ::ammunition-for
  :<- [::inventory/sorted]
  (fn [items [_ _weapon]]
    (->> items
         (filter (comp (partial = :ammunition) :type))
         seq)))

; returns a set of weapon kind ids that should always be treated
; as "finesse" weapons
(reg-sub
  ::finesse-weapon-kinds
  :<- [:sheet-engine-state]
  :<- [:meta/options]
  :<- [:classes]
  (fn [[engine-state options classes]]
    (->> classes
         ; combine all class-specific lists
         (mapcat (fn [c]
                   (when-let [kinds (get-in c [:lists :finesse-weapon-kinds])]
                     (engine/inflate-list
                       engine-state c options kinds))))

         (map (fn [{:keys [id] :as feature}]
                (cond
                  (or (nil? id)
                      (not (keyword? id)))
                  (js/console.warn
                    "WARN: invalid id (from finesse-weapon-kinds): "
                    feature)

                  ; unpack eg :proficiency/longsword
                  (namespace id)
                  (keyword (name id))

                  ; regular weapon kind id
                  :else
                  id)))
         set)))

(defn- compute-bonus [effects modifiers buff]
  (when (fn? buff)
    (buff {:effects effects :modifiers modifiers})))

(defn calculate-weapon
  [proficient-cats proficient-kinds
   effects-set modifiers
   ; NOTE we have to provide a type hint for the compiler
   ; here for some reason....
   ^number proficiency-bonus,
   atk-bonuses dmg-bonuses
   finesse-weapon-kinds
   w]
  (let [{weap-bonus :+
         :keys [kind category ranged? finesse?]} w

        ; we can be proficient in either the weapon's specific kind
        ; (eg :longbow) or its category (eg :martial)
        proficient? (or (proficient-kinds kind)
                        (proficient-cats category))

        ; some classes can treat certain weapon kinds as
        ; finesse even if they aren't naturally (eg: monk)
        finesse? (or finesse?
                     (contains? finesse-weapon-kinds
                                kind))

        ; we can use dex bonus if it's a ranged weapon OR if it's
        ; finesse?
        dex-bonus (when (or ranged? finesse?)
                    (:dex modifiers))

        ; we can use str bonus only for melee weapons
        str-bonus (when (not ranged?)
                    (:str modifiers))

        ; this is also added to the atk roll
        chosen-bonus (max dex-bonus str-bonus)

        prof-bonus (when proficient?
                     proficiency-bonus)

        weap-type-key (if ranged?
                        :ranged
                        :melee)

        dmg-bonus-maps (concat (->> dmg-bonuses weap-type-key vals)
                               (->> dmg-bonuses :any vals))
        atk-bonuses (->> atk-bonuses weap-type-key vals)

        ; raw bonus maps {:+,:when-versatile?}
        other-bonus (->> dmg-bonus-maps
                         (filter #(= (:when-two-handed? %)
                                     (boolean (:two-handed? w)))))

        other-bonus-any (->> other-bonus
                             (filter #(:when-versatile? % true))
                             (keep :+))
        other-bonus-versatile (->> other-bonus
                                   (filter :when-versatile?)
                                   (keep :+))
        other-bonus-non-versatile (->> other-bonus
                                       (remove :when-versatile?)
                                       (keep :+))

        computed-dmg-bonuses (->> dmg-bonus-maps
                                  (keep (partial compute-bonus effects-set modifiers))
                                  (apply +))

        ; TODO indicate dmg type?
        other-dice-bonuses (keep :dice dmg-bonus-maps)

        stat-bonus (let [b (+ weap-bonus chosen-bonus)]
                     (when (not= b 0)
                       b))

        dam-bonus (when stat-bonus
                    stat-bonus
                    (apply + stat-bonus other-bonus-any))

        ; fn that accepts a coll of constant bonus values
        ; and generates a string of "+ <bonus>" including
        ; all other dice bonuses, etc.
        dam-bonuses (fn [const-bonuses]
                      (let [base (->> (cons
                                        (apply + dam-bonus computed-dmg-bonuses const-bonuses)
                                        other-dice-bonuses)
                                      (keep identity)
                                      (str/join " + "))]
                        (when-not (str/blank? base)
                          (str " + " base))))]

    ; NOTE I don't *think* weapon damage ever scales?
    (assoc w
           :base-dice (str (:dice w) (dam-bonuses other-bonus-non-versatile))
           :alt-dice (when-let [versatile (:versatile w)]
                       (str versatile (dam-bonuses other-bonus-versatile)))
           :to-hit (apply + stat-bonus prof-bonus atk-bonuses))))

(reg-sub
  ::equipped-weapons
  :<- [::inventory/equipped]
  :<- [::inventory/eq-proficiencies]
  :<- [::proficiency/bonus]
  :<- [:effect-ids-set]
  :<- [::abilities/modifiers]
  :<- [:wish.sheets.dnd5e.subs/buff-attrs :atk]
  :<- [:wish.sheets.dnd5e.subs/buff-attrs :dmg]
  :<- [::finesse-weapon-kinds]
  (fn [[all-equipped
        proficiencies proficiency-bonus
        effects-set modifiers
        atk-bonuses dmg-bonuses
        finesse-weapon-kinds]]
    (let [{proficient-kinds :kinds
           proficient-cats :categories} proficiencies]
      (->> all-equipped
           (filter #(= :weapon (:type %)))
           (map
             (partial calculate-weapon
                      proficient-cats proficient-kinds
                      effects-set modifiers
                      proficiency-bonus
                      atk-bonuses dmg-bonuses
                      finesse-weapon-kinds))))))

; ======= combat actions ==================================

(reg-sub
  ::actions-for-type
  :<- [:sheet-engine-state]
  :<- [:classes]
  :<- [:races]
  :<- [::inventory/attuned]
  :<- [:effects]
  (fn [[data-source & entity-lists] [_ filter-type]]
    (->> entity-lists
         flatten
         (mapcat
           (fn [c]
             (map (fn [[id flags]]
                    (with-meta
                      (let [action (or (when (= id (:id c))
                                         ; attuned equipment, probably
                                         c)
                                       (feature-by-id data-source c id)
                                       (js/console.warn
                                         "Could not find " filter-type
                                         " with id " id))]

                        (-> action
                            (update :desc (fn [d]
                                            (if (fn? d)
                                              (d c)
                                              d)))
                            (assoc :wish/container c)))

                      (cond
                        (map? flags) flags
                        (keyword? flags) {flags true}
                        :else nil)))
                  (get-in c [:attrs filter-type]))))
         (keep identity)
         (sort-by :name))))

(reg-sub
  ::special-actions
  :<- [::actions-for-type :special-action]
  (fn [actions _]
    (filter
      #(:combat (meta %))
      actions)))

(reg-sub
  ::other-attacks
  :<- [:sheet-engine-state]
  :<- [:meta/options]
  :<- [::abilities/modifiers]
  :<- [::proficiency/bonus]
  :<- [:total-level]
  :<- [:races]
  :<- [:classes]
  :<- [:effects]
  (fn [[data-source options modifiers
        prof-bonus total-level
        & entity-lists] _]
    (->> entity-lists
         flatten
         (keep (juxt (comp :attacks :attrs)
                     identity))

         ; transform from seq of maps of attack-maps into
         ; a seq of attack-maps, including the :wish/source
         ; each came from
         (mapcat (fn [[attacks-map source]]
                   (when-let [attacks (seq attacks-map)]
                     ; NOTE: attacks is a sequence of [id attack-map] pairs
                     (map (fn [[id attack]]
                            (assoc attack
                                   :id id
                                   ; provide :total-level if it doesn't have :level
                                   :wish/source
                                   (update source :level #(or % total-level))))
                          attacks))))

         ; fill out each map, inflating :dice values, etc. and
         ; merging in &from-options as necessary
         (map (fn [attack]
                (let [source-entity (:wish/source attack)
                      context-feature (feature-by-id data-source source-entity (:id attack))
                      attack (if-let [from-option (:&from-option attack)]
                               ; merge in
                               (let [opt (->> options
                                              from-option
                                              first
                                              (src-util/inflate-feature
                                                data-source source-entity))]
                                 (merge
                                   opt

                                   ; use the context-feature's desc, if possible
                                   (when context-feature
                                     (select-keys
                                       context-feature
                                       [:desc]))

                                   attack))

                               ; nothing to do
                               attack)

                      info (assoc source-entity
                                  :modifiers modifiers
                                  :prof-bonus prof-bonus)
                      dice-fn (:dice attack)
                      save-fn (:save-dc attack)]
                  (-> attack
                      (assoc :dmg (dice-fn info))
                      (assoc :save-dc (save-fn info)))))))))

