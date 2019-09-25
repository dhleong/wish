(ns wish.sheets.dnd5e.views.abilities
  (:require [spade.core :refer [defattrs]]
            [wish.style.flex :as flex :refer [flex]]
            [wish.style.shared :refer [metadata]]
            [wish.util :refer [<sub click>evt]]
            [wish.sheets.dnd5e.overlays :as overlays]
            [wish.sheets.dnd5e.overlays.short-rest :as short-rest]
            [wish.sheets.dnd5e.style :as styles]
            [wish.sheets.dnd5e.subs :as subs]
            [wish.sheets.dnd5e.subs.abilities :as abilities]
            [wish.sheets.dnd5e.subs.proficiency :as proficiency]
            [wish.sheets.dnd5e.data :refer [labeled-abilities]]
            [wish.sheets.dnd5e.util :refer [mod->str]]
            [wish.sheets.dnd5e.views.shared
             :refer [buff-value->kind section]]))


(defn- proficiency-style [& {:as extra}]
  [:.proficiency
   (merge {:position 'relative
           :width "10px"
           :height "10px"}
          extra)
   [:&::before
    {:content "' '"
     :width "8px"
     :height "8px"
     :border-radius "50%"
     :border (str "1px solid " styles/color-proficient)
     :background styles/color-proficient
     :display 'inline-block
     :position 'absolute
     :visibility 'hidden}]
   [:&.proficient::before
    {:visibility 'visible}]
   [:&.expert::before
    {:background-color styles/color-expert
     :border-color styles/color-expert}]

   ; this is the semi-circle
   [:&.half::before
    {:width "4px"
     :border-color "#fff"
     :border-radius "8px 0 0 8px"
     :margin-right "4px"
     }]
   ; this is the outline of the circle
   [:&.half::after
    {:content "' '"
     :width "8px"
     :height "8px"
     :border (str "1px solid " styles/color-proficient)
     :border-radius "50%"
     :display 'inline-block
     :position 'absolute
     }] ])


; ======= rest buttons ====================================

(defattrs rest-buttons-style []
  (merge flex/center
         {:margin "8px 0"})

  [:.button (merge
              flex/grow
              styles/button
              styles/text-center)])

(defn rest-buttons []
  [:div (rest-buttons-style)
   [:div.button.short
    {:on-click (click>evt [:toggle-overlay [#'short-rest/overlay]])}
    "Short Rest"]
   [:div.button.long
    {:on-click (click>evt [:toggle-overlay [#'overlays/long-rest-overlay]])}
    "Long Rest"]])


; ======= abilities ========================================

(defattrs abilities-section-style []
  ; make the mod a bit more prominent if we have room
  (at-media
    {:min-width "1000px"}
    [:.abilities>.ability>.mod {:font-size "2em"}])

  {:margin-top "1em"}

  [:.abilities (merge flex
                      {:justify-content 'space-around})]

  [:&>.info (merge metadata
                   styles/text-center)]

  [:.ability (merge flex/vertical
                    flex/center
                    flex/align-center
                    styles/text-center
                    styles/button)
   [:&.buffed
    [:.score {:color "#0d0"}]
    [:.mod {:color "#0d0"}]]
   [:&.nerfed
    [:.score {:color "#d00"}]
    [:.mod {:color "#d00"}]]

   [:.label (merge flex/grow
                   {:font-size "0.7em"})]
   [:.mod {:font-size "1.5em"}]
   [:.score {:font-size "0.9em"
             :margin-bottom "8px"} ] ]

  [:.save flex/center
   [:.label {:font-size "0.4em"
             :transform "rotate(90)"}]
   [:.info (merge metadata
                  {:padding "0 4px"})]
   [:.mod {:font-size "1.05em"}]

   (proficiency-style
     :margin-left "4px")]

  [:.extras metadata])

(defn abilities-display
  ([abilities] (abilities-display abilities false))
  ([abilities clickable?]
   [:<>
    (for [[id label] labeled-abilities]
      (let [{:keys [score modifier mod]} (get abilities id)]
        ^{:key id}
        [:div.ability {:class (when mod
                                (case mod
                                  :buff "buffed"
                                  :nerf "nerfed"))
                       :on-click (when clickable?
                                   (click>evt [:toggle-overlay
                                               [#'overlays/ability-tmp
                                                id
                                                label]]))}
         [:div.label label]
         [:div.mod modifier]
         [:div.score "(" score ")"]
         ]))]))

(defn abilities-section []
  (let [abilities (<sub [::abilities/info])]
    [:div (abilities-section-style)
     [:div.abilities
      [abilities-display abilities :clickable]]

     [:div.info "Saves"]

     [:div.abilities
      (for [[id _label] labeled-abilities]
        (let [{:keys [save proficient?]} (get abilities id)]
          ^{:key id}
          [:span.save
           [:div.mod save]
           [:div.proficiency
            {:class (when proficient?
                      "proficient")}]]))]

     ; This is a good place for things like Elven advantage
     ; on saving throws against being charmed
     (when-let [save-extras (<sub [::proficiency/ability-extras])]
       [:ul.extras
        (for [item save-extras]
          ^{:key (:id item)}
          [:li (:desc item)])])]))


; ======= Skills ===========================================

(def single-column-skills [:.base-ability
                           {:width "3em !important"}])


(defattrs skills-section-style []
  ; collapse into a single row on smaller devices
  ; that can't fit two columns of Skills
  (at-media
    (merge styles/media-tablets
           styles/media-not-smartphones)
    single-column-skills)

  (at-media
    {:max-width "370px"}
    single-column-skills)

  (at-media
    (merge styles/media-smartphones
           {:min-width "371px"})
    [:.skill-col {:max-width "48%"}])

  (at-media
    styles/media-laptops
    [:.skill-col:first-child {:margin-right "12px"}])

  [:.sections
   {:justify-content 'space-between}]

  [:.skill-col (merge
                 flex/vertical
                 flex/grow)
   [:.skill (merge flex
                   flex/wrap
                   {:padding "2px 0"})
    [:.base-ability (merge metadata
                           {:width "100%"})]
    [:.label flex/grow]
    [:.score {:padding "0 4px"}
     [:&.buffed {:color styles/color-accent2}]
     [:&.nerfed {:color styles/color-accent-nerf}]]

    (proficiency-style
      :transform "translate(0, 34%)")]])

(def ^:private skills-table
  [[[:acrobatics "Acrobatics"]
    [:animal-handling "Animal Handling"]
    [:arcana "Arcana"]
    [:athletics "Athletics"]
    [:deception "Deception"]
    [:history "History"]
    [:insight "Insight"]
    [:intimidation "Intimidation"]
    [:investigation "Investigation"]]
   [[:medicine "Medicine"]
    [:nature "Nature"]
    [:perception "Perception"]
    [:performance "Performance"]
    [:persuasion "Persuasion"]
    [:religion "Religion"]
    [:sleight-of-hand "Sleight of Hand"]
    [:stealth "Stealth"]
    [:survival "Survival"]]])

(defn skill-box
  [id label {:keys [ability modifier expert? half? proficient?]}]
  [:div.skill
   [:div.base-ability
    (str "(" (name ability) ")")]
   [:div.label label]

   (let [buffs (<sub [::subs/buffs id])]
     [:div.score (buff-value->kind buffs)
      (mod->str (+ modifier
                   buffs))])
   [:div.proficiency
    {:class [(when (and half?
                        (not (or expert? proficient?)))
               "half")
             (when (or expert? half? proficient?)
               "proficient")
             (when expert?
               "expert")]}]])

(defn skills-section []
  (let [skills (<sub [::abilities/skill-info])]
    (->> skills-table
         (map
           (fn [col]
             [:div.skill-col
              (for [[skill-id label] col]
                ^{:key skill-id}
                [skill-box skill-id label (get skills skill-id)])]))
         (into [:div.sections]))))

; ======= Proficiencies ===================================

(defattrs proficiencies-section-style []
  [:.section {:padding "0 8px"
              :margin-bottom "16px"}]
  [:.item (merge metadata
                 {:display 'inline-block})
   ["&:not(:last-child)" {:padding-right "0.5em"}
    [:&:after {:content "','"}]]])

(defn proficiencies-section []
  [:div (proficiencies-section-style)
   (when-let [proficiencies (seq (<sub [::proficiency/others]))]
     [:<>
      [:h3 "Proficiencies"]

      ; TODO organize by type
      [:div.section
       (for [f proficiencies]
         ^{:key (:id f)}
         [:div.item
          (:name f)])]])

   (when-let [languages (seq (<sub [::proficiency/languages]))]
     [:<>
      [:h3 "Languages"]

      [:div.section
       (for [f languages]
         ^{:key (:id f)}
         [:div.item
          (:name f)])]])])


; ======= public interface ================================

(defn view
  "This is the left side on desktop and tablets, or the
   first page/tab on mobile"
  []
  [:<>
   [abilities-section]

   [rest-buttons]

   [section "Skills"
    (skills-section-style)
    [skills-section]]

   [proficiencies-section]])
