(ns ^{:author "Daniel Leong"
      :doc "dnd5e.style"}
  wish.sheets.dnd5e.style
  (:require [garden.color :as color]
            [spade.core :refer [defclass defattrs]]
            [wish.style.flex :as flex :refer [flex]]
            [wish.style.media :as media]
            [wish.style.shared :refer [metadata]]))

(def color-accent "#fbc02d")
(def color-accent2 "#6f49b9")
(def color-accent-nerf "#b9496f")

(def color-proficient "#77E731")
(def color-expert "#E8E154")

(def color-platinum "#f3f3f3")
(def color-gold "#e3b17b")
(def color-electrum "#6b7a85")
(def color-silver "#a6a4a0")
(def color-copper "#a77c65")

(def button {:cursor 'pointer})

(def text-center {:text-align 'center})

(def disabled-button {:font-style 'italic
                      :color "rgba(1,1,1, 0.25) !important"
                      :cursor 'default})

;;
;; 35/65 layout
;;

(defattrs container []
  (merge flex/vertical
         {:height "100%"}))

(defattrs layout []
  (at-media media/smartphones
    (merge flex/justify-center
           {:width "100%"})
    [:.side {:width "92% !important"}])

  (at-media media/tablets
    (merge flex/justify-center
           {:width "100%"})
    [:.nav>.section {:font-size "1.5em"}])

  (at-media media/not-smartphones
    [:.side {:height "100%"
             :overflow-y 'auto}])

  (at-media media/laptops
    [:.side {:padding "0 !important"}
     [:&.right {:width "700px !important"}]])

  (merge flex
         flex/wrap
         flex/grow
         {:align-self 'center
          :justify-content 'space-around
          :height "100%"
          :width "100%" ; ensure we stretch as wide as we can
          :max-width "1100px"})

  [:.nav (merge flex
                {:overflow-x 'auto})
   [:.section (merge button
                     {:padding "0 4px"})
    [:&.selected {:cursor 'default
                  :color color-accent}]]]

  [:.side {:padding "0 1%"}]
  [:.left {:width "35%"
           :max-width "400px"}]
  [:.right {:width "61%"
            :max-width "700px"}])



;;
;; Widgets
;;

(defclass currency-preview []
  (merge flex/center
         {:font-size "10pt"})

  [:&.large {:font-size "1.2em"
             :padding "12px"}]

  [:.pair {:margin "0 4px"}
   [:.currency {:margin "0 4px"
                :padding "0 4px"
                :color "#fff"
                :font-size "80%"
                :border-radius "12px" }
    [:&.p {:background-color color-platinum}]
    [:&.g {:background-color color-gold}]
    [:&.e {:background-color color-electrum}]
    [:&.s {:background-color color-silver}]
    [:&.c {:background-color color-copper}]]])

(defattrs inventory-quantity []
  (merge flex/center
         flex/justify-center)

  [:input.quantity {:font-size "120%"
                    :width "3em"
                    :text-align 'center}])

(defclass cast-spell []
  (merge text-center
         {:width "3.5em"
          :padding "4px"
          :margin "0 8px 0 0"})
  [:&.upcast {:position 'relative
              :border (str "2px solid " color-accent2)}
   [:&:hover {:background-color "#f0f0f0"
              :color "#333"}]
   [:&:hover>.upcast-level
    {:background (color/lighten color-accent2 20)}]
   [:.upcast-level {:position 'absolute
                    :background color-accent2
                    :border-radius "2px"
                    :color "#fff !important"
                    :padding "0.2em"
                    :right 0
                    :bottom "-0.7em"
                    :font-size "0.5em"
                    :transform "translate(50%, 0)"}]]
  [:.uses-remaining {:padding "0.1em"
                     :font-size "0.7em"
                     :font-style 'italic}]

  [:&.button
   [:&.disabled disabled-button]
   [:&.nested:hover {:background-color "#f0f0f0"
                     :color "#333"}]])

(defattrs spell-card []
  {:max-width "300px"}

  [:table.info metadata
   [:th.header {:text-align 'right}]]
  [:.desc {:font-size "90%"}]

  [:.upcast {:color color-accent2}]

  [:.cast-container (merge flex
                           flex/justify-center)]

  [:.spell-leveling flex/center
   [:.btn button
    [:&.disabled {:cursor 'default
                  :color "#ccc"}]]
   [:.level {:font-size "140%"
             :padding "12px"}]])

(defattrs spell-tags []
  [:.tag {:margin "0 4px"
          :padding "0 4px"
          :color "#fff"
          :font-size "80%"
          :border-radius "12px"
          :background-color "#333"}])



(defattrs swipeable-page []
  {:min-height "60vh"})

(defattrs consumable-use-block []
  (merge flex/center
         {:font-size "80%"
          :margin-bottom "1em"
          :padding "8px 8px 0 8px"})

  [:.uses {:padding "4px"}])

;;
;; Sections
;;

(defattrs actions-section []
  (at-media media/smartphones
            [:.filters {:justify-content 'center}])
  [:.filters (merge flex
                    {:border-bottom "1px solid #333"
                     :margin-bottom "8px"
                     :overflow-x 'hidden})
   ["&:not(:first-child)"
    {:margin-top "32px"}]
   [:.filter {:padding "4px"}
    [:.unselectable {:font-weight 'bold}]
    [:a {:font-size "75%"}]]]

  [:.combat-info (merge metadata
                        {:margin-bottom "8px"})
   [:&.effects {:color color-accent2}
    [:a {:color color-accent2}
     [:&:hover {:color (color/lighten color-accent2 20)}]]]
   [:.item
    ["&:not(:first-child)" {:padding-right "0.5em"}
     [:&:before {:content "'·'"
                 :display 'inline-block
                 :text-align 'center
                 :width "1em"}]]]
   [:.effects
    {:float 'right}]]

  [:.attack flex/center
   [:.name flex/grow]
   [:.info-group (merge flex/center
                        flex/vertical-center
                        {:padding "4px"})
    [:.label {:font-size "60%"}]
    [:.dmg
     [:&.buffed {:color color-accent2}]
     [:&.nerfed {:color color-accent-nerf}]
     [:.alt {:font-size "65%"}]]]]
  [:.ammo (merge metadata
                 flex/center
                 button
                 {:margin-left "16px"})
   [:.amount {:padding "0 12px"}]
   [:.consume {:padding "4px"}]]

  [:.spells (merge flex/center
                   flex/wrap
                   {:margin-bottom "12px"})

   [:.section-label {:font-size "90%"
                     :font-weight 'bold}]
   [:.spell-name {:font-size "80%"
                  :font-style 'italic
                  :padding "4px"}]]
  [:.action
   [:.name {:font-size "90%"
            :font-weight 'bold
            :font-style 'italic}]
   [:.desc metadata]])

(defattrs features-section []
  [:.features-category>h3 {:border-bottom "1px solid #000"}]
  [:.feature {:margin-bottom "1em"}
   [:.name {:font-weight 'bold}]
   [:.chosen {:font-size "1em"
              :overflow 'hidden
              :text-overflow 'ellipsis
              :white-space 'nowrap
              }]]
  [:.desc (merge metadata
                 {:margin "0 8px"})
   [:p:first-child {:margin-top "0"}]
   [:p:last-child {:margin-bottom "0"}]]
  [:.chosen-details {:margin "4px 16px"
                     :align-self 'flex-start}
   [:h5 {:margin 0}]])

(defattrs limited-use-section []
  [:.limited-use (merge
                   flex/center
                   {:padding "4px"})
   [:.info flex/grow
    [:.recovery metadata]]
   [:.usage
    [:.button (merge button)
     [:&.selected {:background-color "#ddd"}
      [:&:hover {:background-color "#eee"}]]]

    [:input.uses-left (merge text-center
                             {:width "3em"})]

    [:.many flex/center
     [:.modify {:padding "8px"}
      [:&.disabled disabled-button]]]]])

(defattrs spells-section []
  [:.spell-slot-level flex/center
   [:.label flex/grow]]

  [:.spellcaster-info {:font-size "80%"
                       :font-weight 'normal
                       :margin-left "8px"}
   [:.item
    ["&:not(:first-child)" {:padding-right "0.5em"}
     [:&:before {:content "'·'"
                 :display 'inline-block
                 :text-align 'center
                 :width "1em"}]]]]

  [:.manage-link {:font-weight 'normal
                  :font-size "80%"}]

  [:.spell flex/center
   [:.upcast {:color color-accent2}]
   [:.meta metadata]
   [:.spell-info flex/grow
    [:.name {:font-weight "bold"}]]
   [:.dice (merge text-center
                  {:align-self 'center}) ]] )

(defattrs inventory-section []
  [:.add {:padding "12px 8px"}
   [:.link {:padding "8px"}]]

  [:.expandable>.button {:background 'none
                         :border-top "1px solid #333"
                         :color "#000"
                         :padding 0}]
  [:.item (merge flex/center
                 {:font-size "80%"
                  :padding "4px 8px"})
   [:&.equipped.attuned {:background-color "#99cc66f0"}]
   [:&.equipped {:background-color "#66cccc80"}]
   [:&.attuned {:background-color "#cccc0080"}]

   [:.edit {:margin-right "12px"}]

   [:.info flex/grow
    [:.notes-preview {:overflow 'hidden
                      :font-size "10px"
                      :height "12px"
                      :max-width "150px"
                      :padding-right "1em"
                      :text-overflow 'ellipsis
                      :white-space 'pre}]]
   [:.button {:font-size "60%"}
    [:&.disabled disabled-button]
    [:&:hover {:background-color "#f0f0f0"
               :color "#333"}
     [:&.disabled {:background-color "#ccc"}]]]]
  [:.item-info {:padding "0 12px"
                :font-size "90%"
                :text-align 'justify}
   [:.delete (merge flex/center
                    flex/justify-center
                    {:align-self 'center
                     :margin "12px 0"})]] )


