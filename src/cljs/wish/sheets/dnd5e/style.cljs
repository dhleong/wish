(ns ^{:author "Daniel Leong"
      :doc "dnd5e.style"}
  wish.sheets.dnd5e.style
  (:require [cljs-css-modules.macro :refer-macros [defstyle]]
            [wish.style.flex :as flex :refer [flex]]
            [wish.style.shared :refer [metadata]]))

(def color-proficient "#77E731")
(def color-expert "#E8E154")

(def button {:cursor 'pointer})

(def proficiency-style
  [:.proficiency
   {:color color-proficient
    :padding-right "12px"}
   [:&::before
    {:content "'●'"
     :visibility 'hidden}]
   [:&.proficient::before
    {:visibility 'visible}]
   [:&.expert::before
    {:color color-expert}]])

(defstyle styles
  [:.header (merge flex
                   {:background "#666666"
                    :color "#f0f0f0"
                    :padding "4px 12px"})
   [:.side flex
    [:.col (merge flex/vertical-center
                  {:padding "4px 8px"
                   :text-align 'center})
     [:&.left {:text-align 'left}]

     [:.meta (merge flex
                    metadata)
      [:.race {:margin-right "0.5em"}]]

     [:.save-state {:margin-right "12px"}]

     [:.stat {:font-size "140%"}
      [:.unit {:font-size "60%"}]]]]

   [:.label {:font-size "80%"}]

   [:.hp {:align-items 'center}
    [:.now {:padding "4px"
            :font-size "120%"
            :text-align 'center}]]

   [:.space flex/grow]]

  [:.hp-overlay {:width "300px"}
   [:.current-hp {:width "5em"
                  :font-size "1.2em"
                  :text-align 'center}]]

  [:.short-rest-overlay {:max-width "400px"}
   [:.desc metadata]]

  [:.abilities
   [:.ability (merge flex
                     flex/align-center
                     {:height "1.7em"})
    [:.score {:font-size "1.1em"
              :width "1.9em"}]
    [:.label flex/grow]
    [:.info (merge metadata
                   {:padding "0 4px"})]
    [:.mod {:font-size "1.1em"
            :padding-right "12px"}]
    proficiency-style]]

  [:.skills
   [:.skill-col (merge
                  flex/vertical
                  flex/grow)
    [:.skill (merge flex
                    flex/wrap
                    {:padding "2px 0"})
     [:.base-ability (merge metadata
                            {:width "100%"})]
     [:.label flex/grow]
     [:.score {:padding "0 4px"}]

     proficiency-style]]]

  [:.combat
   [:.attack flex/center
    [:.name flex/grow]
    [:.info-group (merge flex/center
                         flex/vertical-center
                         {:padding "4px"})
     [:.label {:font-size "60%"}]]]]

  [:.limited-use-section
   [:.rests flex/center
    [:.button (merge
                flex/grow
                button
                {:text-align 'center})]]
   [:.limited-use (merge
                    flex/center
                    {:padding "4px"})
    [:.info flex/grow
     [:.recovery metadata]]
    [:.usage
     [:.button (merge button)
      [:&.selected {:background-color "#ddd"}
       [:&:hover {:background-color "#eee"}]]]]]]

  [:.spells-section
   [:.spell-slots-use flex
    [:.slot {:width "24px"
             :height "24px"
             :border "1px solid #333"
             :margin "4px"}
     [:&.used {:cursor 'pointer}]]]

   [:.spell-slot-level flex/center
    [:.label flex/grow]]

   [:.spell flex/center
    [:.spell-info flex/grow
     [:.name {:font-weight "bold"}]
     [:.meta metadata]]
    [:.dice {:align-self 'center}]]])

