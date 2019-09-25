(ns ^{:author "Daniel Leong"
      :doc "Shared widgets for dnd5e sheet "}
  wish.sheets.dnd5e.widgets
  (:require-macros [wish.util :refer [fn-click]]
                   [wish.util.log :as log :refer [log]])
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [reagent-forms.core :refer [bind-fields]]
            [wish.util :refer [>evt <sub click>evt invoke-callable]]
            [wish.sheets.dnd5e.data :as data]
            [wish.sheets.dnd5e.events :as events]
            [wish.sheets.dnd5e.subs :as subs]
            [wish.sheets.dnd5e.subs.spells :as spells]
            [wish.sheets.dnd5e.subs.inventory :as inventory]
            [wish.sheets.dnd5e.style :as styles]
            [wish.views.widgets :as widgets
             :refer-macros [icon]
             :refer [formatted-text link>evt]]))

(defn stringify-components
  [{components :comp}]
  (when components
    (letfn [(vs-parts [k]
              (case k
                :v "V"
                :s "S"
                :vs "V, S"))
            (m-part [p]
              (str "M (" p ")"))]
      (cond
        (keyword? components)
        (vs-parts components)

        ; are any material only?
        (string? components)
        (m-part components)

        (= 1 (count components))
        (m-part (first components))

        :else
        (str (vs-parts (first components))
             ", "
             (m-part (second components)))))))

(defn- stringify-dam-type
  [t]
  ; TODO table?
  (when t
    (-> t
        name
        str/capitalize
        (str/replace "-" " "))))

(defn currency-preview [& [large?]]
  (let [{:keys [platinum gold silver electrum copper]} (<sub [::inventory/currency])
        any? (> (+ platinum gold silver electrum copper)
                0)]
    [:span {:class [(when large?
                      "large")
                    styles/currency-preview]}

     [:div.label "Currency:"]

     (when-not any?
       " (just some lint)")

     (when (> platinum 0)
       [:div.pair
        platinum
        [:span.currency.p "P"]])
     (when (> gold 0)
       [:div.pair
        gold
        [:span.currency.g "G"]])
     (when (> electrum 0)
       [:div.pair
        electrum
        [:span.currency.e "E"]])
     (when (> silver 0)
       [:div.pair
        silver
        [:span.currency.s "S"]])
     (when (> copper 0)
       [:div.pair
        copper
        [:span.currency.c "C"]])]))

(defn item-quantity-manager [item]
  [:div styles/inventory-quantity
   [:a.modify {:href "#"
               :on-click (click>evt [:inventory-subtract item 1])}
    (icon :remove-circle)]

   [bind-fields

    [:input.quantity {:field :numeric
                      :id :quantity
                      :min 0}]

    {:get #(<sub [::inventory/item-quantity (:id item)])
     :save! (fn [_path v]
              (>evt [:inventory-set-amount item v]))}]

   [:a.modify {:href "#"
               :on-click (click>evt [:inventory-add item 1])}
    (icon :add-circle)] ])


; ======= Spells-related ==================================

(defn cast-button
  "Renders a button to cast the given spell at its current level.
   Renders a box with 'At Will' if the spell is a cantrip"
  ([s] (cast-button nil s))
  ([{:keys [base-level upcastable? nested?]
     :or {upcastable? true}} s]
   (let [cantrip? (= 0 (:spell-level s))
         at-will? (or cantrip?
                      (:at-will? s))
         base-level (or base-level
                        (:spell-level s))

         use-slot? (= (:consumes s) :*spell-slot)
         use-id (when-not use-slot?
                  (:consumes s))

         ; if it's not at-will (or consumes a limited-use)
         ; try to figure out what slot we can use
         ; (the sub handles the check)
         {slot-level :level
          slot-kind :kind
          slot-remain :unused
          slot-total :total} (<sub [::spells/usable-slot-for s])

         castable-level (if cantrip?
                          0  ; always
                          (when (or upcastable?
                                    (= (:spell-level s)
                                       slot-level))
                            slot-level))

         uses-left (when use-id
                     (:uses-left (<sub [::subs/limited-use use-id])))

         has-uses? (or cantrip?
                       (if use-id
                         (when uses-left
                           (> uses-left 0))

                         ; normal spell; if there's a castable-level for it,
                         ; we're good to go
                         (not (nil? castable-level))))

         uses-remaining (when-not cantrip?
                          (if use-id
                            uses-left
                            slot-remain))

         upcast? (when has-uses?
                   (> castable-level base-level))]

     (if at-will?
       ; easy case; at-will spells don't need a "cast" button
       [:div.cast {:class styles/cast-spell}
        "At Will"]

       [:div.cast.button
        {:class [styles/cast-spell
                 (when nested?
                   "nested")
                 (when-not has-uses?
                   "disabled")
                 (when upcast?
                   "upcast")]
         :on-click (fn-click [e]
                     (when has-uses?
                       (.stopPropagation e)
                       (if use-id
                         (do
                           (log "Consume " s " VIA " use-id)
                           (>evt [:+use use-id 1]))
                         (do
                           (log "Cast " s " AT " castable-level)
                           (>evt [::events/use-spell-slot
                                  slot-kind slot-level slot-total])))))}

        ; div content:
        (if (or use-id use-slot?)
          "Use"
          "Cast")

        (when uses-remaining
          [:div.uses-remaining
           uses-remaining " left"])

        (when upcast?
          [:span.upcast-level
           " @" (get data/level-suffixed castable-level)])
        ]))))

(defn spell-aoe
  "Renders the AOE of a spell"
  [[kind l w]]
  [:span
   (case kind
     :cylinder (str l "-foot-radius, " w " ft. tall cylinder")

     :circle (str l "-foot-radius circle")
     :cone (str l " ft. cone")
     :sphere (str l "-foot-radius sphere")

     :cube (str l " ft. cube")
     :line (str l " ft. long, " w " ft. wide line")
     :square (str l " ft. square"))
   ])

(defn spell-tags
  "Render a series of 'tags' indicating things like whether
   the spell is a Ritual, or requires Concentration."
  [{:keys [con? rit?]}]
  (when (or con? rit?)
    [:span styles/spell-tags
     (when con?
       [:span.tag "C"])
     (when rit?
       [:span.tag "R"])
     ]))

(defn- opt-row [s key-fn title]
  (when-let [v (key-fn s)]
    [:tr
     [:td.header title]
     [:td v]]))

(defn- the-spell-card
  [{:keys [update-level! base-level]}
   {:keys [spell-level prepared?] :as s}]
  (let [cantrip? (= 0 spell-level)
        all-slots (<sub [::spells/usable-slots-for (assoc s :spell-level base-level)])
        min-castable-level (->> all-slots first :level)
        max-level (->> all-slots last :level)
        {cast-level :level} (<sub [::spells/usable-slot-for s])
        spell-level (max cast-level spell-level)
        s (-> s
              (assoc :spell-level spell-level)
              (update :spell-mod #(or % "(spell mod)")))
        upcast? (not= spell-level base-level)
        upcast-class {:class (when upcast?
                               "upcast")}]
    [:div styles/spell-card
     [:table.info
      [:tbody
       (opt-row s :time "Casting Time")
       (opt-row s :range "Range")

       (when-let [aoe (:aoe s)]
         [:tr
          [:th.header "Area of Effect"]
          [:td [spell-aoe aoe]]])

       (opt-row s stringify-components "Components")
       (opt-row s :duration "Duration")
       (opt-row s spell-tags "Properties")

       (when (:dice s)
         (let [{:keys [damage]} s
               dice-value (invoke-callable s :dice)
               base-dice (if (not= spell-level base-level)
                           (invoke-callable
                             (-> s
                                 (assoc :spell-level base-level)
                                 (update :spell-mod #(or % "(spell mod)")))
                             :dice))
               buff-extras (when-let [buffs (:buffs s)]
                             (when-let [buff (buffs s)]
                               (str " + " buff)))
               upcast-class (when (not= base-dice
                                        dice-value)
                              upcast-class)]
           [:tr
            [:th.header upcast-class
             (if damage
               "Damage"
               "Healing")]

            [:td
             [:u.dice upcast-class
              dice-value
              buff-extras]

             (if-let [dam-type (stringify-dam-type damage)]
               (str " " dam-type)
               " HP")]]))

       (when-let [save (:save s)]
         [:tr
          [:th.header
           "Saving Throw"]
          [:td
           (case save
             :str "Strength"
             :dex "Dexterity"
             :con "Constitution"
             :int "Intelligence"
             :wis "Wisdom"
             :cha "Charisma")
           (when-let [dc (:save-dc s)]
             (str " (" dc ")"))]])

       [:tr
        [:th.header upcast-class
         "Spell Level"]
        [:td
         [:div.spell-leveling
          (when-not cantrip?
            [link>evt {:class ["btn"
                               (when (or (<= spell-level
                                             min-castable-level)
                                         (<= spell-level
                                             base-level))
                                 "disabled")]
                       :on-click (fn-click
                                   (update-level! dec))}
             (icon :remove-circle)])

          ; the current level
          (if cantrip?
            "Cantrip"
            [:div.level upcast-class
             spell-level])

          (when-not cantrip?
            [link>evt {:class ["btn"
                               (when (>= spell-level
                                         max-level)
                                 "disabled")]
                       :on-click (fn-click
                                   (update-level! inc))}
             (icon :add-circle)])]]]
       ]]

     (when (or cantrip?
               (not= false prepared?))
       [:div.cast-container
        [cast-button {:base-level base-level} s]])

     [formatted-text :div.desc (:desc s)]]))

(defn spell-card
  "Spell info card widget"
  [s]
  (r/with-let [base-level (:spell-level s)
               spell-atom (r/atom s)]
    [the-spell-card
     {:base-level base-level
      :update-level!
      (fn [f]
        (swap! spell-atom
               update
               :spell-level
               (fn [old-level]
                 ; this is somewhat obnoxiously complicated
                 ; since we need to skip over fully-used
                 ; slot levels :\
                 (let [slots (<sub [::spells/usable-slots-for s])
                       old-index (->> slots
                                      (keep-indexed
                                        (fn [i {:keys [level]}]
                                          (when (= level old-level)
                                            i)))
                                      first)
                       new-index (min (dec (count slots))
                                      (max 0 (f old-index)))]
                   (:level
                     (nth slots
                          new-index))))))}
     @spell-atom]))
