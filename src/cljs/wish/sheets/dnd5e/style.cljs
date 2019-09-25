(ns ^{:author "Daniel Leong"
      :doc "dnd5e.style"}
  wish.sheets.dnd5e.style
  (:require [garden.color :as color]
            [spade.core :refer [defclass defattrs]]
            [wish.style.flex :as flex :refer [flex]]
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

(def media-tiny {:screen :only
                 :max-width "375px"})
(def media-smartphones {:screen :only
                        :max-width "479px"})
(def media-not-smartphones {:min-width "480px"})
(def media-tablets {:max-width "1024px"})
(def media-laptops {:min-width "1100px"})

(def button {:cursor 'pointer})

(def text-center {:text-align 'center})

(def one-third
  {:width "100%"})

(def base-overlay {:padding "32px"})
(def overlay (merge base-overlay
                    {:width "300px"}))

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
  (at-media
    media-smartphones
    (merge flex/justify-center
           {:width "100%"})
    [:.side {:width "92% !important"}])

  (at-media
    media-tablets
    (merge flex/justify-center
           {:width "100%"})
    [:.nav>.section {:font-size "1.5em"}])

  (at-media
    media-not-smartphones
    [:.side {:height "100%"
             :overflow-y 'auto}])

  (at-media
    {:min-width "1100px"}
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
;; Overlays
;;

(defattrs ability-tmp-overlay []
  overlay

  [:.number {:font-size "110%"
             :margin-left "8px"
             :width "3em"}])

(defattrs currency-manager-overlay []
  (at-media media-tiny
            {:padding "0 !important"}
            [:.p
             [:span.label {:display 'none}]
             [:&:after {:content "'Plat'"}]]
            [:.e
             [:span.label {:display 'none}]
             [:&:after {:content "'Elec'"}]])

  base-overlay

  [:h5 {:padding-left "16px"}]
  [:.meta (merge metadata
                 {:max-width "180px"
                  :padding-left "16px"})]
  [:table (merge text-center
                 flex/vertical
                 flex/align-center
                 {:border-collapse 'collapse})]
  [:th.header {:font-size "80%"}
   [:&.p {:background-color color-platinum}]
   [:&.g {:background-color color-gold}]
   [:&.e {:background-color color-electrum}]
   [:&.s {:background-color color-silver}]
   [:&.c {:background-color color-copper}]]
  [:.amount {:width "4em"
             :text-align 'center}]
  [:div.apply {:margin-top "12px"
               :text-align 'center}])

(defattrs custom-item-overlay []
  overlay

  [:.section {:padding "8px"}
   [:&.flex (merge flex/center
                   {:justify-content 'space-between})]
   [:&.limited-use {:margin "0 12px"}]]
  [:.stretch {:width "100%"}]
  [:.error {:border "1px solid #aa0000"
            :padding "4px"
            :font-size "80%"}
   [:.close {:display 'none}]]
  [:input.numeric {:width "2em"
                   :font-size "110%"}])

(defattrs hp-overlay []
  (at-media media-smartphones
            [:.quick-adjust :.new-hp
             {:width "15vw !important"}]
            [:.quick-adjust (merge flex/vertical
                                   flex/align-center)
             [:.number {:width "15vw !important"}]
             [:.label {:font-size "80%"
                       :overflow-x 'visible
                       :white-space 'nowrap}]])
  overlay

  [:.current-hp (merge text-center
                       {:width "5em"
                        :font-size "1.2em"})]
  [:.centered text-center]
  [:.section-header {:margin-bottom "0px"}]

  [:.new-hp (merge text-center
                   {:padding "12px"
                    :width "4em"})
   [:.label {:font-size "80%"}]
   [:.amount {:font-size "140%"}
    [:&.healing {:color "#00cc00"}]
    [:&.damage {:color "#cc0000"}]]
   [:input.apply (merge
                   button
                   {:margin-top "1em"})]]
  [:.quick-adjust (merge text-center
                         {:padding "4px"})
   [:.number (merge text-center
                    {:font-size "1.2em"
                     :width "4em"})]]

  [:.none (merge metadata
                 {:padding "8px"})]
  [:.condition {:margin "4px 0"}
   [:.expandable :.header {:padding "4px 0"}
    [:ul.per-levels {:margin "4px"}]]
   [:.name flex/center
    [:.delete {:margin-right "4px"}]
    [:.meta {:margin-left "4px"}]]
   [:.desc metadata]])

(defattrs info-overlay []
  overlay

  [:table.info (merge metadata
                      {:margin-top "8px"})
   [:th.header {:text-align 'right}]]
  [:.desc metadata])

(defattrs item-adder-overlay []
  (at-media media-smartphones
            [:.item-browser {:height "70vh !important"}])

  overlay

  [:.search-bar {:margin-bottom "8px"}]
  [:.item-browser {:height "250px"
                   :padding "4px"}]
  [:.item (merge flex/center
                 {:font-size "80%"
                  :min-height "2.3em"})
   [:.name flex/grow]
   [:.button {:font-size "60%"}
    [:&.disabled disabled-button]
    [:&:hover {:background-color "#f0f0f0"
               :color "#333"}
     [:&.disabled {:background-color "#ccc"}]]]] )

(defattrs notes-overlay []
  (at-media
    media-smartphones
    [:textarea.notes {:height "80vh"
                      :font-size "10pt !important"}])

  (merge base-overlay
         {:width "80vw"})

  [:textarea.notes {:width "100%"
                    :font-size "12pt"
                    :min-height "50vh"}])

(defattrs short-rest-overlay []
  (merge base-overlay
         {:max-width "400px"})

  [:.sections {:margin-bottom "1em"
               :justify-content 'start}
   [:.hit-dice-pool {:margin-right "2em"}
    [:.hit-die button]]
   [:.hit-die-use flex/center
    [:.hit-die-value {:width "5em"}]]]
  [:.desc metadata])

(defattrs spell-info-overlay []
  base-overlay)

(defattrs spell-management-overlay []
  base-overlay

  [:.limit metadata]
  ;; [:.stretch (merge flex/grow
  ;;                   {:width "100%"})]
  [".spell:not(.unavailable) + .unavailable" {:margin-top "12px"}
   [:&.with-button {:margin-bottom "12px"}]
   [:&:before {:width "100%"
               :height "1px"
               :margin-bottom "20px"
               :background color-accent-nerf
               :content "''"
               :display 'block}]]
  [:.spell {:padding "4px 0"}
   [:.header flex]
   [:.meta metadata]
   [:.info (merge flex/grow
                  button) ]
   [:.prepare (merge button
                     text-center
                     {:min-width "5em"})
    [:&.disabled {:cursor 'default
                  :color "#999"}]]])

(defattrs starting-equipment-overlay []
  overlay

  [:.alternatives {:border "1px solid #333"
                   :margin "4px 0"
                   :overflow-x 'hidden
                   :padding "4px"}
   [:.choice {:color "rgba(0,0,0, 0.5)"
              :padding "4px"}
    ["&:nth-child(n+2)" {:position 'relative
                         :padding-top "12px"}
     [:&:before {:content "''"
                 :position 'absolute
                 :top "2px"
                 :width "98%"
                 :border-bottom "1px solid #999"}]
     [:&:after {:background-color "#f0f0f0"
                :color "#999"
                :content "'OR'"
                :font-size "10px"
                :position 'absolute
                :padding "0 8px"
                :top "-3px"
                :left "3em"
                }]]
    [:&.chosen {:color "#000"}]]]
  [:.pack
   [:.contents metadata]]
  [:.accept {:padding "8px"
             :font-size "120%"}])

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
  (at-media media-smartphones
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


