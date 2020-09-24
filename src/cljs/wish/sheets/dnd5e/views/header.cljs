(ns wish.sheets.dnd5e.views.header
  (:require [clojure.string :as str]
            [spade.core :refer [defattrs]]
            [wish.style :refer [text-primary-on-dark]]
            [wish.style.flex :as flex :refer [flex]]
            [wish.style.media :as media]
            [wish.util :refer [<sub click>evt]]
            [wish.util.nav :refer [sheet-url]]
            [wish.sheets.dnd5e.overlays :as overlays]
            [wish.sheets.dnd5e.overlays.hp
             :refer [overlay] :rename {overlay hp-overlay}]
            [wish.sheets.dnd5e.style :as styles]
            [wish.sheets.dnd5e.subs :as subs]
            [wish.sheets.dnd5e.subs.effects :as effects]
            [wish.sheets.dnd5e.subs.hp :as hp]
            [wish.sheets.dnd5e.subs.combat :as combat]
            [wish.sheets.dnd5e.subs.proficiency :as proficiency]
            [wish.sheets.dnd5e.util :refer [mod->str]]
            [wish.sheets.dnd5e.views.shared :refer [buff-kind->attrs]]
            [wish.views.widgets :as widgets
             :refer-macros [icon]
             :refer [link link>evt]]))

(defattrs header-container-style []
  (at-media media/dark-scheme
    {:background "#333"})

  {:display 'block
   :background "#666666"})

(defattrs header-style []
  (at-media media/tablets
    [:.col.meta {:max-width "15vw"}])

  (at-media media/smartphones
    [:.side
     [:&.settings {:order "0 !important"}]
     [:&.right {:justify-content 'space-between
                :padding "0 12px"
                :width "100%"}]]

    [:.col.meta {:max-width "35vw"}]

    [:.hp
     [:.label
      [:.content {:display 'none}]
      [:&:after {:content "'HP'"}]]
     [:.value {:display "block !important"}]
     [:.divider {:display 'block
                 :height "1px"
                 :border-top "1px solid #fff"
                 :overflow 'hidden}]
     [:.max {:font-size "60%"}]])

  (at-media media/tiny
    {:font-size "80%"}
    [:.side {:padding "0 !important"}])

  [:& (merge flex
             flex/wrap
             {:color text-primary-on-dark
              :margin "0 auto"
              :padding "4px 0"
              :max-width "1200px"
              :width "100%"})]
  [:.side flex
   [:&.left {:padding-left "12px"}]
   [:&.settings {:order 1
                 :padding-right "12px"}]

   [:.col (merge flex/vertical-center
                 styles/text-center
                 {:padding "4px 8px"})
    [:&.left {:text-align 'left}]

    [:.meta (merge flex
                   flex/wrap
                   {:font-size "80%"})
     [:.race {:margin-right "0.5em"}]]

    [:.save-state {:margin-right "12px"}]

    [:.stat {:font-size "140%"}
     [:&.buffed {:color styles/color-accent2}]
     [:&.nerfed {:color styles/color-accent-nerf}]
     [:.unit {:font-size "60%"}]]]]

  [:.label {:font-size "80%"}]

  [:.hp flex/center
   [:.value (merge flex
                   styles/text-center
                   {:padding "4px"
                    :font-size "120%"})]
   [:.divider {:padding "0 4px"}]
   [:.indicators
    [:.icon {:font-size "12px"}
     [:&.save {:color "#00cc00"}]
     [:&.fail {:color "#aa0000"}]]]
   [:.max
    [:&.buffed {:color styles/color-accent2}]
    [:&.nerfed {:color styles/color-accent-nerf}]]]

  [:.space flex/grow])

; ======= Top bar ==========================================

(defn- hp-normal [hp max-hp hp-mod]
  (let [buff-kind (<sub [::effects/change-for :hp-max])]
    [:<>
     [:div.label [:span.content "Hit Points"]]
     [:div.value
      [:div.now hp]
      [:div.divider " / "]
      [:div.max (buff-kind->attrs (or hp-mod buff-kind))
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
  (let [[hp max-hp hp-mod] (<sub [::hp/state])]
    [:div.clickable.hp.col
     {:on-click (click>evt [:toggle-overlay [#'hp-overlay]])}

     (if (> hp 0)
       [hp-normal hp max-hp hp-mod]
       [hp-death-saving-throws])]))

(defn buffable-stat [stat-id label & content]
  (let [buff-kind (<sub [::effects/change-for stat-id])]
    [:div.col
     (into [:div.stat (buff-kind->attrs buff-kind)]
           content)
     [:div.label label]]))


; ======= public interface ================================

(defn view []
  (let [common (<sub [:sheet-meta])
        classes (<sub [:classes])]
    [:div (header-container-style)
     [:div (header-style)
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

