(ns wish.sheets.dnd5e.subs.spells
  (:require [clojure.string :as str]
            [re-frame.core :as rf :refer [reg-sub subscribe]]
            [wish-engine.core :as engine]
            [wish.sheets.dnd5e.util :as util :refer [ability->mod ]]
            [wish.sheets.dnd5e.subs.abilities :as abilities]
            [wish.sheets.dnd5e.subs.inventory :as inventory]
            [wish.sheets.dnd5e.subs.util
             :refer [filter-by-str options-of-list]]
            [wish.util :refer [invoke-callable distinct-by ->map ]]
            [wish.util.string :as wstr]))

; ======= Constants ========================================

(def spell-slot-schedules
  {:standard
   {1 {1 2}
    2 {1 3}
    3 {1 4, 2 2}
    4 {1 4, 2 3}
    5 {1 4, 2 3, 3 2}
    6 {1 4, 2 3, 3 3}
    7 {1 4, 2 3, 3 3, 4 1}
    8 {1 4, 2 3, 3 3, 4 2}
    9 {1 4, 2 3, 3 3, 4 3, 5 1}
    10 {1 4, 2 3, 3 3, 4 3, 5 2}
    11 {1 4, 2 3, 3 3, 4 3, 5 2, 6 1}
    12 {1 4, 2 3, 3 3, 4 3, 5 2, 6 1}
    13 {1 4, 2 3, 3 3, 4 3, 5 2, 6 1, 7 1}
    14 {1 4, 2 3, 3 3, 4 3, 5 2, 6 1, 7 1}
    15 {1 4, 2 3, 3 3, 4 3, 5 2, 6 1, 7 1, 8 1}
    16 {1 4, 2 3, 3 3, 4 3, 5 2, 6 1, 7 1, 8 1}
    17 {1 4, 2 3, 3 3, 4 3, 5 2, 6 1, 7 1, 8 1, 9 1}
    18 {1 4, 2 3, 3 3, 4 3, 5 3, 6 1, 7 1, 8 1, 9 1}
    19 {1 4, 2 3, 3 3, 4 3, 5 3, 6 2, 7 1, 8 1, 9 1}
    20 {1 4, 2 3, 3 3, 4 3, 5 3, 6 2, 7 2, 8 1, 9 1}}

   :standard/half
   {2 {1 2}
    3 {1 3}
    4 {1 3}
    5 {1 4, 2 2}
    6 {1 4, 2 2}
    7 {1 4, 2 3}
    8 {1 4, 2 3}
    9 {1 4, 2 3, 3 2}
    10 {1 4, 2 3, 3 2}
    11 {1 4, 2 3, 3 3}
    12 {1 4, 2 3, 3 3}
    13 {1 4, 2 3, 3 3, 4 1}
    14 {1 4, 2 3, 3 3, 4 1}
    15 {1 4, 2 3, 3 3, 4 2}
    16 {1 4, 2 3, 3 3, 4 2}
    17 {1 4, 2 3, 3 3, 4 3, 5 1}
    18 {1 4, 2 3, 3 3, 4 3, 5 1}
    19 {1 4, 2 3, 3 3, 4 3, 5 2}
    20 {1 4, 2 3, 3 3, 4 3, 5 2}}

   :multiclass
   {1 {1 2}
    2 {1 3}
    3 {1 4, 2 2}
    4 {1 4, 2 3}
    5 {1 4, 2 3, 3 2}
    6 {1 4, 2 3, 3 3}
    7 {1 4, 2 3, 3 3, 4 1}
    8 {1 4, 2 3, 3 3, 4 2}
    9 {1 4, 2 3, 3 3, 4 3, 5 1}
    10 {1 4, 2 3, 3 3, 4 3, 5 2}
    11 {1 4, 2 3, 3 3, 4 3, 5 2, 6 1}
    12 {1 4, 2 3, 3 3, 4 3, 5 2, 6 1}
    13 {1 4, 2 3, 3 3, 4 3, 5 2, 6 1, 7 1}
    14 {1 4, 2 3, 3 3, 4 3, 5 2, 6 1, 7 1}
    15 {1 4, 2 3, 3 3, 4 3, 5 2, 6 1, 7 1, 8 1}
    16 {1 4, 2 3, 3 3, 4 3, 5 2, 6 1, 7 1, 8 1}
    17 {1 4, 2 3, 3 3, 4 3, 5 2, 6 1, 7 1, 8 1, 9 1}
    18 {1 4, 2 3, 3 3, 4 3, 5 3, 6 1, 7 1, 8 1, 9 1}
    19 {1 4, 2 3, 3 3, 4 3, 5 3, 6 2, 7 1, 8 1, 9 1}
    20 {1 4, 2 3, 3 3, 4 3, 5 3, 6 2, 7 2, 8 1, 9 1}}})

(def std-slots-label "Spell Slots")


; ======= spellcasting-related subscriptions ==============

(reg-sub
  :5e/spells-filter
  (fn [db]
    (:5e/spells-filter db nil)))


(defn knowable-spell-counts-for
  [spellcaster modifiers]
  (let [{:keys [id level cantrips slots known]} spellcaster

        spells (cond
                 known (get known (dec level))

                 ; NOTE: :standard is the default if omitted
                 (or (nil? slots)
                     (= :standard slots)) (max
                                            1
                                            (+ level
                                               (get modifiers id)))

                 (= :standard/half slots) (max
                                            1
                                            (+ (js/Math.ceil (/ level 2))
                                               (get modifiers id))))

        cantrips (->> cantrips
                      (partition 2)
                      (reduce
                        (fn [cantrips-known [at-level incr]]
                          (if (>= level at-level)
                            (+ cantrips-known incr)
                            (reduced cantrips-known)))
                        0))]

    {:spells spells
     :cantrips cantrips}))


; returns eg: {:cleric {:spells 4, :cantrips 2}}
(reg-sub
  ::knowable-spell-counts-by-class
  :<- [::spellcaster-blocks]
  :<- [::spellcasting-modifiers]
  (fn [[spellcasters modifiers]]
    (reduce
      (fn [m s]
        (assoc m (:id s) (knowable-spell-counts-for s modifiers)))
      {}
      spellcasters)))

(reg-sub
  ::knowable-spell-counts
  :<- [::knowable-spell-counts-by-class]
  (fn [counts [_ class-id]]
    (get counts class-id)))

; NOTE: this function is an absolute beast, and could benefit from
; some unit testing for sure, and probably some refactoring. It's
; responsible for handling all the possible caster types, different
; ways of acquiring spells (IE: in spellbook, provided by class
; features, etc)
(defn inflate-prepared-spells-for-caster
  [total-level engine-state modifiers
   prof-bonus spell-buffs options
   caster-attrs]
  (let [attrs caster-attrs
        caster-id (:id attrs)
        c (:wish/container attrs)
        {:keys [acquires? prepares?]} attrs
        spells-list (:spells attrs)
        extra-spells-list (:extra-spells attrs)

        ; NOTE: we namespace spell mods by the class/race id in case
        ; we ever want to combine all :attrs of a character into
        ; a single map.
        spell-mods (get-in c [:attrs :spells caster-id])

        ; if we acquire AND prepare spells, the source list is still
        ; the same, but we use the :acquires?-spells option-list to
        ; determine which are actually prepared (the normal :spells
        ; list just indicates which spells are *acquired*)
        spells-option (cond
                        ; if an explicit list was provided, use it
                        (:prepared-spells attrs)
                        (:prepared-spells attrs)

                        (and acquires? prepares?)
                        (:acquires?-spells attrs)

                        ; the normal list
                        :else spells-list)

        ; FIXME it should actually be the intersection of acquired
        ; and prepared, in case they un-learn a spell but forget
        ; to un-prepare it. This is an edge case, but we should
        ; be graceful about it. Alternatively, unlearning a spell
        ; should also eagerly un-prepare it.

        ; all spells from the extra-spells list
        ; NOTE: because extra spells are provided
        ; by features and levels, we can't find them
        ; in the data source.
        ; ... unless it's a collection of spell ids
        extra-spells (when extra-spells-list
                       (engine/inflate-list
                         engine-state c options extra-spells-list))

        ; extra spells are always prepared
        extra-spells (some->> extra-spells
                              (map #(assoc % :always-prepared? true
                                           ::extra-spell? true)))

        selected-spell-ids (get options spells-option [])

        ; only selected spells from the main list (including those
        ; added by class features, eg warlock)
        selected-spells (engine/inflate-list
                          engine-state c options selected-spell-ids)

        ; for :acquires? spellcasters, their acquired
        ; cantrips are always prepared
        selected-spells (if acquires?
                          (->> (engine/inflate-list
                                 engine-state c options (get options spells-list []))
                               (filter #(= 0 (:spell-level %)))
                               (map #(assoc % :always-prepared? true))

                               ; plus manually prepared spells
                               (concat selected-spells))

                          selected-spells)]

    (->> (concat
           extra-spells
           selected-spells)

         (map #(-> %
                   (assoc
                     ::source caster-id
                     :total-level total-level
                     :spell-mod (get modifiers caster-id)
                     :save-label (when-let [k (:save %)]
                                   (str/upper-case
                                     (name k)))

                     :buffs (if (:damage %)
                              (:dmg spell-buffs)
                              (:healing spell-buffs))

                     ; save dc is 8 + prof + ability
                     :save-dc (+ 8
                                 prof-bonus
                                 (get modifiers caster-id)
                                 (:saves spell-buffs)))
                   (merge (get spell-mods (:id %)))))

         ; sort by level, then name
         (sort-by (juxt :spell-level :name)))))

(reg-sub
  ::prepared-spells-by-class
  :<- [:sheet-engine-state]
  :<- [::spellcaster-blocks]
  :<- [:total-level]
  :<- [::spellcasting-modifiers]
  :<- [:wish.sheets.dnd5e.subs/proficiency-bonus]
  :<- [::spell-buffs]
  :<- [:meta/options]
  (fn [[engine-state spellcasters total-level modifiers
        prof-bonus spell-buffs options]]
    (some->> spellcasters
             seq
             (reduce
               (fn [m {caster-id :id :as attrs}]
                 (assoc m caster-id
                        (inflate-prepared-spells-for-caster
                          total-level engine-state modifiers
                          prof-bonus spell-buffs options
                          attrs)))
               {}))))

; count of explicitly acquired (IE not added by class features, etc.)
; spells in the given spells-list
(reg-sub
  ::acquired-spells-count
  :<- [:sheet-engine-state]
  :<- [:meta/options]
  (fn [[data-source options] [_ spells-list]]
    (let [selected-ids (get options spells-list)]
      (->> (engine/inflate-list data-source selected-ids)
           (filter #(not= 0 (:spell-level %)))
           count))))

; just (get [::prepared-spells-by-class] class-id)
(reg-sub
  ::prepared-spells
  :<- [::prepared-spells-by-class]
  (fn [by-class [_ class-id]]
    (get by-class class-id)))

(reg-sub
  ::prepared-spells-filtered
  :<- [::all-prepared-spells]
  (fn [spells [_ filter-type]]
    (when-not (= :special-action filter-type)
      (filter (case filter-type
                :action (fn [s]
                          (and (not (util/bonus-action? s))
                               (not (util/reaction? s))))
                :bonus util/bonus-action?
                :reaction util/reaction?

                (if (number? filter-type)
                  #(= filter-type (:level %))

                  (throw (js/Error.
                           (str "Unknown spell filter-type:" filter-type)))))
              spells))))

; reduces ::prepared-spells into {:cantrips, :spells},
; AND removes ones that are always prepared
(reg-sub
  ::my-prepared-spells-by-type

  (fn [[_ class-id]]
    (subscribe [::prepared-spells class-id]))

  (fn [spells]
    (->> spells
         (remove ::extra-spell?)
         (reduce
           (fn [m s]
             (let [spell-type (if (= 0 (:spell-level s))
                                :cantrips
                                :spells)]
               (update m spell-type conj s)))
           {:cantrips []
            :spells []}))))

; list of all spells on a given spell list for the given spellcaster block,
; with `:prepared? bool` inserted as appropriate
(reg-sub
  ::preparable-spell-list

  (fn [[_ spellcaster _list-id]]
    [(subscribe [:sheet-engine-state])
     (subscribe [:meta/options])
     (subscribe [::prepared-spells (:id spellcaster)])
     (subscribe [::highest-spell-level-for-spellcaster-id (:id spellcaster)])
     (subscribe [:5e/spells-filter])])

  (fn [[engine-state options prepared-spells highest-spell-level
        filter-str]
       [_ spellcaster list-id include-unavailable?]]
    (let [; is this the prepared list for an acquires? spellcaster?
          acquires-list? (= (:acquires?-spells spellcaster)
                            list-id)

          ; are we listing spells that an acquires? spellcaster *can* acquire?
          acquire-mode? (and (:acquires? spellcaster)
                             (not acquires-list?))

          prepared-set (if acquire-mode?
                         ; in acquire mode, the "prepared set" is actually
                         ; the "acquired set"
                         (get options (:spells spellcaster) #{})

                         (->> prepared-spells
                              (map :id)
                              set))

          ; for an acquires? spellcaster, their cantrips are "always prepared,"
          ; but we should be able to un-acquire them in case of mis-clicks, etc.
          always-prepared (->> (if acquire-mode?
                                 (->> prepared-spells
                                      (remove #(= 0 (:spell-level %))))

                                 ; not acquire-mode; all :always-prepared?
                                 ; go into the set
                                 prepared-spells)
                               (filter :always-prepared?))
          always-prepared-set (->> always-prepared
                                   (map :id)
                                   set)

          source (if acquires-list?
                   ; if we want to look at the :acquired? list, its
                   ; source is actually the selected from (:spells)
                   ; NOTE: do we need to concat class-provided lists?
                   (->> (options-of-list
                          engine-state (:spells spellcaster)
                          (get options (:spells spellcaster) #{})))

                   ; normal case:
                   (engine/inflate-list
                     engine-state (:wish/container spellcaster)
                     options
                     list-id))

          source (if-let [filter-fn (:values-filter spellcaster)]
                   ; let the spellcaster determine the filter
                   (filter (fn [spell]
                             (filter-fn (assoc spell :level (:level spellcaster))))
                           source)

                   ; limit visible spells by those actually available
                   ; (IE it must be of a level we can prepare)
                   (if include-unavailable?
                     (map (fn [s]
                            (assoc s :unavailable?
                                   (> (:spell-level s) highest-spell-level)))
                          source)

                     (filter #(<= (:spell-level %) highest-spell-level)
                             source)))]

      (->> source
           (concat always-prepared)
           (distinct-by :id)

           (filter-by-str filter-str)

           ; sort by level then name
           (sort-by (juxt :spell-level :name))

           (map #(if (always-prepared-set (:id %))
                   (assoc % :always-prepared? true)
                   %))
           (map #(if (prepared-set (:id %))
                   (assoc % :prepared? true)
                   %))))))

; list of all prepared spells across all classes
(reg-sub
  ::all-prepared-spells
  :<- [::prepared-spells-by-class]
  (fn [spells-by-class]
    (->> spells-by-class
         vals
         flatten)))

(reg-sub
  ::spell-attacks
  :<- [::spell-attack-bonuses]
  :<- [::all-prepared-spells]
  (fn [[bonuses prepared-spells]]
    (->> prepared-spells
         (filter :attack)
         (map (fn [s]
                (assoc s
                       :to-hit (get bonuses
                                    (::source s))

                       :base-dice (invoke-callable
                                    s
                                    :dice)))))))

(reg-sub
  ::spellcaster-blocks
  :<- [:classes]
  :<- [:races]
  (fn [spellcaster-collections]
    (->> spellcaster-collections
         flatten
         (mapcat (fn [c]
                   (when-let [sc-map (-> c :attrs :5e/spellcaster)]
                     (map (fn [[id spellcaster]]
                            (-> spellcaster
                                (assoc :id id
                                       :wish/container c
                                       :level (:level c))
                                (cond->
                                  (not (:name spellcaster))
                                  (assoc :name (:name c)))))
                          sc-map)))))))

(reg-sub
  ::spellcaster-blocks-by-id
  :<- [::spellcaster-blocks]
  (fn [blocks]
    (->map blocks)))

(reg-sub
  ::spellcaster-block-by-id
  :<- [::spellcaster-blocks-by-id]
  (fn [blocks [_ spellcaster-id]]
    (get blocks spellcaster-id)))

(reg-sub
  ::spellcaster-info
  :<- [::spellcasting-modifiers]
  :<- [::spell-attack-bonuses]
  :<- [:wish.sheets.dnd5e.subs/proficiency-bonus]
  :<- [::spell-buffs]
  (fn [[modifiers atk-bonuses prof-bonus {save-buffs :saves}]
       [_ spellcaster-id]]
    (let [atk (get atk-bonuses spellcaster-id)
          modifier (get modifiers spellcaster-id)]
      {:mod modifier
       :attack atk
       :save-dc (+ 8 prof-bonus modifier save-buffs)})))

(reg-sub
  ::spellcasting-modifiers
  :<- [::abilities/all]
  :<- [::spellcaster-blocks]
  (fn [[abilities spellcasters]]
    (reduce
      (fn [m c]
        (let [spellcasting-ability (-> c :ability)]
          (assoc m
                 (:id c)
                 (ability->mod
                   (get abilities spellcasting-ability)))))
      {}
      spellcasters)))

(reg-sub
  ::eq-attack-buffs
  :<- [::inventory/attuned]
  (fn [eq _]
    (->> eq
         (filter (fn [{id :id}]
                   ; hacks?
                   (some (partial str/starts-with? (name id))
                         ["rod-" "staff-" "wand-"])))
         (map :+)
         (apply +))))

(reg-sub
  ::spell-attack-bonuses
  :<- [::spellcasting-modifiers]
  :<- [:wish.sheets.dnd5e.subs/proficiency-bonus]
  :<- [::eq-attack-buffs]
  (fn [[modifiers proficiency-bonus buffs]]
    (reduce-kv
      (fn [m class-or-race-id modifier]
        (assoc m class-or-race-id (+ proficiency-bonus modifier buffs)))
      {}
      modifiers)))

(defn comp-buffs
  "Returns a composed function that computes the buffs
   for a given spell"
  [all-buffs]
  (let [consts (->> all-buffs
                    (filter number?)
                    (apply +))
        fns (->> all-buffs
                 (filter fn?))]
    (fn compute-buffs [s]
      (let [buffs (+ consts
                     (reduce
                       (fn [total f]
                         (+ total (f s)))
                       0
                       fns))]
        (when-not (= 0 buffs)
          buffs)))))

(reg-sub
  ::spell-buffs
  :<- [:all-attrs]
  (fn [attrs]
    (->> attrs
         :buffs
         :spells
         (reduce-kv
           (fn [m kind buffs-map]
             (assoc m kind
                    (->> buffs-map
                         vals
                         comp-buffs)))
           {}))))

(defn- standard-spell-slots? [c]
  (= :standard (:slots-type c :standard)))

(defn spell-slots
  [spellcasters]
  (if (= 1 (count spellcasters))
    (let [spellcaster (first spellcasters)
          level (:level spellcaster)
          kind (:slots-type spellcaster :standard)
          label (:slots-label spellcaster std-slots-label)
          schedule (:slots spellcaster :standard)
          schedule (or (when (keyword? schedule)
                         (schedule spell-slot-schedules))
                       schedule)]
      (if-not (= :none schedule)
        {kind {:label label
               :slots (get schedule level)}}
        (select-keys spellcaster [:cantrips])))

    (let [std-level (apply
                      +
                      (->> spellcasters
                           (filter standard-spell-slots?)
                           (map
                             (fn [c]
                               (let [mod (:multiclass-levels-mod c 1)]
                                 (when-not (= mod 0)
                                   (int
                                     (Math/floor
                                       (/ (:level c)
                                          mod)))))))))]
      (apply
        merge
        {:standard
         {:label std-slots-label
          :slots (get-in spell-slot-schedules [:multiclass std-level])}}
        (->> spellcasters
             (filter (complement standard-spell-slots?))
             (map (fn [c]
                    (spell-slots [c]))))))))

(reg-sub
  ::spellcaster-blocks-with-slots
  :<- [::spellcaster-blocks]
  (fn [all-blocks]
    (filter #(not= :none (:slots %))
            all-blocks)))

(reg-sub
  ::spell-slots
  :<- [::spellcaster-blocks-with-slots]
  spell-slots)

(reg-sub
  ::spell-slots-for-spellcaster-id
  (fn [[_ spellcaster-id]]
    (subscribe [::spellcaster-block-by-id spellcaster-id]))
  (fn [spellcaster]
    (spell-slots [spellcaster])))

(defn highest-spell-level-for-slots [slots]
  (or (when (map? (:slots slots))
        (some->> slots
                 :slots

                 ; highest spell level available
                 keys
                 (apply max)))

      (when (> (some->> slots :cantrips second) 0)
        0)))

(reg-sub
  ::highest-spell-level-for-spellcaster-id
  (fn [[_ spellcaster-id]]
    (subscribe [::spell-slots-for-spellcaster-id spellcaster-id]))
  (fn [slots-by-type]
    (highest-spell-level-for-slots
      ; NOTE: we only have one slot type to look at since we're looking
      ; at a specific spellcaster type
      (some->> slots-by-type vals first))))

(reg-sub
  ::highest-spell-level
  :<- [::spell-slots]
  (fn [all-slots]
    (->> all-slots
         vals
         (mapcat (comp keys :slots))
         (apply max))))

(reg-sub
  ::spellcaster-slot-types
  :<- [::spellcaster-blocks-with-slots]
  (fn [classes]
    (->> classes
         (filter (complement standard-spell-slots?))
         (map #(name (:slots-type %)))
         set)))

(reg-sub
  ::spell-slots-used
  :<- [:limited-used]
  :<- [::spellcaster-slot-types]
  (fn [[used slot-types]]
    (reduce-kv
      (fn [m id used]
        (let [id-ns (namespace id)
              level (-> id name wstr/last-char int)]
          (cond
            (= "slots" id-ns)
            (assoc-in m [:standard level] used)

            (contains? slot-types id-ns)
            (assoc-in m [(keyword id-ns) level] used)

            :else m)))  ; ignore; unrelated
      {}
      used)))

(defn available-slots
  [slots used]
  (->> slots
       (map identity) ; convert to [kind, {info}]
       (mapcat
         (fn [[kind {levels :slots}]]
           (keep (fn [[level total]]
                   (let [unused (- total
                                   (get-in used [kind level]))]
                     (when (> unused 0)
                       {:kind kind
                        :level level
                        :total total
                        :unused unused})))
                 levels)))
       (sort-by :level)))

; returns [{:kind,:level,:total,:unused}]
; sorted in ascending order by :level
(reg-sub
  ::available-slots
  :<- [::spell-slots]
  :<- [::spell-slots-used]
  (fn [[slots used] _]
    (available-slots slots used)))

(defn usable-slots-for
  [slots spellcaster s]
  (let [{use-id :consumes
         :keys [spell-level at-will?]} s
        limited-use? (and use-id
                          (not= :*spell-slot use-id))
        cantrip? (= 0 spell-level)
        at-will? (or cantrip? at-will?)
        no-slots? (= :none (:slots spellcaster))]
    (when-not (or limited-use? at-will?
                  ; no-slot casters can still have a slot
                  ; if it's a spell-slot-based limited use
                  (when-not (= :*spell-slot use-id)
                    no-slots?))
      ; if it's at-will or powered by a limited-use,
      ; there's no possible usable slot because it doesn't
      ; use *any* slots.
      ; Of course, if the limited-use is the special
      ; :*spell-slot type, then we *can* use slots
      (->> slots
           (filter #(>= (:level %)
                        spell-level))))))

(reg-sub
  ::usable-slots-for
  (fn [[_ s]]
    [(subscribe [::available-slots])
     (subscribe [::spellcaster-block-by-id (::source s)])])
  (fn [[slots spellcaster] [_ s]]
    (usable-slots-for slots spellcaster s)))

; returns {:kind, :level} if any
(reg-sub
  ::usable-slot-for
  (fn [[_ s]]
    (subscribe [::usable-slots-for s]))
  (fn [usable-slots _]
    (first usable-slots)))

