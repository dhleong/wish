(ns wish.sheets.dnd5e.views.abilities
  (:require [wish.util :refer [<sub click>evt]]
            [wish.sheets.dnd5e.overlays :as overlays]
            [wish.sheets.dnd5e.style :as styles]
            [wish.sheets.dnd5e.subs :as subs]
            [wish.sheets.dnd5e.data :refer [labeled-abilities]]
            [wish.sheets.dnd5e.util :refer [mod->str]]
            [wish.sheets.dnd5e.views.shared
             :refer [buff-value->kind section]]))

(defn rest-buttons []
  [:div styles/rest-buttons
   [:div.button.short
    {:on-click (click>evt [:toggle-overlay [#'overlays/short-rest-overlay]])}
    "Short Rest"]
   [:div.button.long
    {:on-click (click>evt [:toggle-overlay [#'overlays/long-rest-overlay]])}
    "Long Rest"]])


; ======= abilities ========================================

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
  (let [abilities (<sub [::subs/ability-info])]
    [:div styles/abilities-section
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
     (when-let [save-extras (<sub [::subs/ability-extras])]
       [:ul.extras
        (for [item save-extras]
          ^{:key (:id item)}
          [:li (:desc item)])])]))


; ======= Skills ===========================================

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
  (let [skills (<sub [::subs/skill-info])]
    (->> skills-table
         (map
           (fn [col]
             [:div.skill-col
              (for [[skill-id label] col]
                ^{:key skill-id}
                [skill-box skill-id label (get skills skill-id)])]))
         (into [:div.sections]))))

; ======= Proficiencies ===================================

(defn proficiencies-section []
  [:div styles/proficiencies-section
   (when-let [proficiencies (seq (<sub [::subs/other-proficiencies]))]
     [:<>
      [:h3 "Proficiencies"]

      ; TODO organize by type
      [:div.section
       (for [f proficiencies]
         ^{:key (:id f)}
         [:div.item
          (:name f)])]])

   (when-let [languages (seq (<sub [::subs/languages]))]
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
    styles/skills-section
    [skills-section]]

   [proficiencies-section]])
