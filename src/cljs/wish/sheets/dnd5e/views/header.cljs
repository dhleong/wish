(ns wish.sheets.dnd5e.views.header
  (:require [clojure.string :as str]
            [wish.util :refer [<sub click>evt]]
            [wish.util.nav :refer [sheet-url]]
            [wish.sheets.dnd5e.overlays :as overlays]
            [wish.sheets.dnd5e.style :as styles]
            [wish.sheets.dnd5e.subs :as subs]
            [wish.sheets.dnd5e.subs.hp :as hp]
            [wish.sheets.dnd5e.subs.combat :as combat]
            [wish.sheets.dnd5e.subs.proficiency :as proficiency]
            [wish.sheets.dnd5e.util :refer [mod->str]]
            [wish.sheets.dnd5e.views.shared :refer [buff-kind->attrs]]
            [wish.views.widgets :as widgets
             :refer-macros [icon]
             :refer [link link>evt]]))


; ======= Top bar ==========================================

(defn- hp-normal [hp max-hp]
  (let [buff-kind (<sub [::subs/effect-change-for :hp-max])]
    [:<>
     [:div.label [:span.content "Hit Points"]]
     [:div.value
      [:div.now hp]
      [:div.divider " / "]
      [:div.max (buff-kind->attrs buff-kind)
       max-hp]]]))

(defn- save-indicators
  [prefix icon-class used]
  [:div.indicators
   prefix

   (for [i (range 3)]
     (with-meta
       (if (< i used)
         (icon :radio-button-checked.icon {:class icon-class})
         (icon :radio-button-unchecked.icon {:class icon-class}))
       {:key i}))])

(defn hp-death-saving-throws
  ([] (hp-death-saving-throws nil))
  ([sheet-id]
   (let [{:keys [saves fails]} (<sub [::hp/death-saving-throws sheet-id])]
     [:<>
      [save-indicators "😇" :save saves]
      [save-indicators "☠️" :fail fails]])))

(defn hp []
  (let [[hp max-hp] (<sub [::hp/state])]
    [:div.clickable.hp.col
     {:on-click (click>evt [:toggle-overlay [#'overlays/hp-overlay]])}

     (if (> hp 0)
       [hp-normal hp max-hp]
       [hp-death-saving-throws])]))

(defn buffable-stat [stat-id label & content]
  (let [buff-kind (<sub [::subs/effect-change-for stat-id])]
    [:div.col
     (into [:div.stat (buff-kind->attrs buff-kind)]
           content)
     [:div.label label]]))


; ======= public interface ================================

(defn view []
  (let [common (<sub [:sheet-meta])
        classes (<sub [:classes])]
    [:div styles/header-container
     [:div styles/header
      [:div.left.side
       [:div.col
        [widgets/save-state]]

       [:div.col.left.meta
        [:div.name [link {:class "inline"
                          :href "/sheets"}
                    (:name common)]]
        [:div.meta
         [:div.race (:name (<sub [:race]))]
         [:div.classes (->> classes
                            (map (fn [c]
                                   (str (-> c :name) "\u00a0" (:level c))))
                            (str/join " / "))]]]

       [:div.col
        [link>evt [:toggle-overlay [#'overlays/notes-overlay]]
         (icon :description)]]]

      [:div.space]

      [:div.share.side
       [:div.col
        [widgets/share-button]]]

      [:div.settings.side
       [:div.col
        (let [sheet-id (<sub [:active-sheet-id])]
          [link {:href (sheet-url sheet-id :builder :class)}
           (icon :settings)])]]

      [:div.right.side
       [:div.col
        [:div.stat (mod->str
                     (<sub [::proficiency/bonus]))]
        [:div.label "Proficiency"]]

       [buffable-stat :ac "AC"
        (<sub [::combat/ac])]

       [buffable-stat :speed "Speed"
        (<sub [::subs/speed]) [:span.unit " ft"]]

       [:div.col
        [:div.stat (<sub [::subs/passive-perception])]
        [:div.label "Pass. Perc."]]

       [buffable-stat :initiative "Initiative"
        (mod->str
          (<sub [::combat/initiative]))]

       [hp]]
      ]]))

