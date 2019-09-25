(ns wish.sheets.dnd5e.overlays.spell-management
  (:require-macros [wish.util :refer [fn-click]])
  (:require [reagent.core :as r]
            [wish.sheets.dnd5e.subs.spells :as spells]
            [wish.sheets.dnd5e.overlays.style :as styles]
            [wish.sheets.dnd5e.widgets :refer [spell-card spell-tags]]
            [wish.util :refer [<sub click>evt click>swap!]]
            [wish.views.widgets :as widgets :refer-macros [icon]]
            [wish.views.widgets.fast-numeric]))


; ======= Spell management =================================

(defn spell-info-header [opts s]
  [:div.info opts
   [:div.name (:name s)]
   [:div.meta
    [:span.level
     (if (= 0 (:spell-level s))
       "Cantrip"
       (str "Level " (:spell-level s)))]
    [spell-tags s]]] )

(defn- spell-block
  [s {:keys [selectable?
             source-list
             verb]}]
  (r/with-let [expanded? (r/atom false)]
    [:div.spell {:class (when (:unavailable? s)
                          "unavailable")}
     [:div.header
      [spell-info-header
       {:on-click (click>swap! expanded? not)}
       s]
      (if (:always-prepared? s)
        [:div.prepare.disabled
         {:title "Always Prepared"}
         (icon :check-circle-outline)]

        [:div.prepare
         {:class (when-not (or (:prepared? s)
                               (selectable? s))
                   "disabled")
          :on-click (click>evt [:update-option-set source-list
                                (if (:prepared? s)
                                  disj
                                  conj)
                                (:id s)])}
         (if (:prepared? s)
           (icon :check-circle)
           verb)])]

     (when @expanded?
       [spell-card (update s :prepared? #(or % false))])]))

(defn- spell-management* [spellcaster mode hide-unavailable?]
  (let [{:keys [acquires? prepares?]} spellcaster

        knowable (<sub [::spells/knowable-spell-counts (:id spellcaster)])
        highest-spell-level (<sub [::spells/highest-spell-level-for-spellcaster-id
                                   (:id spellcaster)])

        ; in :acquisition mode (eg: for spellbooks), cantrips have
        ; the normal limit but spells are unlimited
        limits (case mode
                 :default knowable
                 :acquisition (dissoc knowable :spells))

        prepare-verb (cond
                       ; TODO we could add an :acquire-verb...
                       (= :acquisition mode) "Acquire"
                       prepares? "Prepare"
                       :else "Learn")
        prepared-verb (cond
                        ; TODO we could add an :acquired-verb...
                        (= :acquisition mode) "acquired"
                        prepares? "prepared"
                        :else "learned")

        title (case mode
                :default (str "Manage "
                              (:name spellcaster)
                              (if prepares?
                                " Prepared"
                                " Known")
                              " Spells")
                :acquisition (str "Manage "
                                  (:name spellcaster)
                                  " "
                                  (:acquired-label spellcaster)))

        spells-limit (:spells limits)
        cantrips-limit (when-not (and acquires?
                                      (= :default mode))
                         (:cantrips limits))

        available-list (if (and
                             acquires?
                             (not= :acquisition mode))
                         ; for an :acquires? spellcaster in default mode,
                         ; the source for their prepared spells is their
                         ; :acquires?-spells list
                         (:acquires?-spells spellcaster)

                         ; otherwise, it's the :spells list
                         (:spells spellcaster))

        all-prepared (<sub [::spells/my-prepared-spells-by-type (:id spellcaster)])
        prepared-spells-count (count (:spells all-prepared))
        prepared-cantrips-count (count (:cantrips all-prepared))
        total-spells-count (when (and (not spells-limit)
                                      acquires?)
                             (<sub [::spells/acquired-spells-count
                                    available-list]))

        spells (<sub [::spells/preparable-spell-list
                      spellcaster available-list

                      ; include unavailable spells in "prepare" mode normally,
                      ; or "acquisition" mode for acquires? spellcasters
                      (or (not acquires?)
                          (= :acquisition mode))])

        can-select-spells? (or (nil? spells-limit)
                               (< prepared-spells-count spells-limit))
        can-select-cantrips? (< prepared-cantrips-count cantrips-limit)
        ; respect the :prepared-spells option if given
        source-list (:prepared-spells spellcaster
                                      available-list)
        spell-opts (assoc spellcaster
                          :verb prepare-verb
                          :source-list source-list
                          :selectable? (fn [{:keys [spell-level]}]
                                         (when (<= spell-level highest-spell-level)
                                           (if (= 0 spell-level)
                                             can-select-cantrips?
                                             can-select-spells?))))

        any-unavailable? (some :unavailable? spells)
        spells (if @hide-unavailable?
                 (remove :unavailable? spells)
                 spells)]

    [:div (styles/spell-management-overlay)
     [:h5 title
      (if spells-limit
        [:div.limit
         "Spells " prepared-spells-count " / " spells-limit]
        [:div.limit
         "Spells (" (or total-spells-count
                        prepared-spells-count)
         " " prepared-verb ")"])
      (when (> cantrips-limit 0)
        [:div.limit
         "Cantrips " prepared-cantrips-count " / " cantrips-limit])]

     [widgets/search-bar
      {:filter-key :5e/spells-filter
       :placeholder "Search for a spell..."}]

     #_[:div.stretch
        [virtual-list
         :items spells
         :render-item (fn [opts item]
                        [:div.spell-container opts
                         [spell-block item spell-opts]])]]
     (for [s spells]
       ^{:key (:id s)}
       [spell-block s spell-opts])

     (when (and @hide-unavailable? any-unavailable?)
       [:div.unavailable.spell.with-button
        [:div.button {:on-click
                      (fn-click
                        (reset! hide-unavailable? false))}
         "Show unavailable spells"]])
     ]))

(defn overlay [spellcaster & {:keys [mode]
                              :or {mode :default}}]
  (r/with-let [hide-unavailable? (r/atom true)]
    [spell-management* spellcaster mode hide-unavailable?]))
