(ns ^{:author "Daniel Leong"
      :doc "dnd5e.style"}
  wish.sheets.dnd5e.style
  (:require [cljs-css-modules.macro :refer-macros [defstyle]]
            [wish.style.flex :as flex :refer [flex]]
            [wish.style.shared :refer [metadata]]))

(def color-proficient "#77E731")
(def color-expert "#E8E154")

(def color-platinum "#f3f3f3")
(def color-gold "#e3b17b")
(def color-electrum "#6b7a85")
(def color-silver "#a6a4a0")
(def color-copper "#a77c65")

(def media-smartphones {:screen :only
                        :max-width "479px"})
(def media-tiny {:screen :only
                 :max-width "375px"})

(def button {:cursor 'pointer})

(def text-center {:text-align 'center})

(def one-third
  {:width "100%"})

(def base-overlay {:padding "32px"})
(def overlay (merge base-overlay
                    {:width "300px"}))

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

(defstyle header
  (at-media
    media-smartphones
    [:.header
     [:.side
      [:&.settings {:order "0 !important"}]
      [:&.right {:justify-content 'space-between
                 :padding "0 12px"
                 :width "100%"}]]
     [:.hp
      [:.label
       [:.content {:display 'none}]
       [:&:after {:content "'HP'"}]]
      [:.value {:display "block !important"}]
      [:.divider {:display 'block
                  :height "1px"
                  :border-top "1px solid #fff"
                  :overflow 'hidden}]
      [:.max {:font-size "60%"}]]])

  (at-media
    media-tiny
    [:.header {:font-size "80%"}
     [:.side {:padding "0 !important"}]])

  [:.header (merge flex
                   flex/wrap
                   {:background "#666666"
                    :color "#f0f0f0"
                    :padding "4px 0"
                    :width "100%"})
   [:.side flex
    [:&.left {:padding-left "12px"}]
    [:&.settings {:order 1
                  :padding-right "12px"}]

    [:.col (merge flex/vertical-center
                  text-center
                  {:padding "4px 8px"})
     [:&.left {:text-align 'left}]

     [:.meta (merge flex
                    metadata)
      [:.race {:margin-right "0.5em"}]]

     [:.save-state {:margin-right "12px"}]

     [:.stat {:font-size "140%"}
      [:.unit {:font-size "60%"}]]]]

   [:.label {:font-size "80%"}]

   [:.hp flex/center
    [:.value (merge flex
                    text-center
                    {:padding "4px"
                     :font-size "120%"})]
    [:.divider {:padding "0 4px"}]
    [:.indicators
     [:.icon {:font-size "12px"}
      [:&.save {:color "#00cc00"}]
      [:&.fail {:color "#aa0000"}]]]]

   [:.space flex/grow]])

;; TODO refactor all of this please
(defstyle styles
  (at-media
    media-smartphones
    [:.notes-overlay
     [:textarea.notes {:height "80vh"}]])

  [:.spell-tags
   [:.tag {:margin "0 4px"
           :padding "0 4px"
           :color "#fff"
           :font-size "80%"
           :border-radius "12px"
           :background-color "#333"}]]

  [:.ability-tmp-overlay overlay
   [:.number {:font-size "110%"
              :margin-left "8px"
              :width "3em"}]]

  [:.custom-item-overlay overlay
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
                    :font-size "110%"}]]

  [:.hp-overlay overlay
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
    [:.desc metadata]]]

  [:.notes-overlay overlay
   [:textarea.notes {:width "100%"
                     :min-height "50vh"}]]

  [:.short-rest-overlay (merge base-overlay
                               {:max-width "400px"})
   [:.sections {:margin-bottom "1em"
                :justify-content 'start}
    [:.hit-dice-pool {:margin-right "2em"}
     [:.hit-die button]]
    [:.hit-die-use flex/center
     [:.hit-die-value {:width "5em"}]]]
   [:.desc metadata]]

  [:.spell-info-overlay base-overlay]

  [:.spell-management-overlay base-overlay
   [:.limit metadata]
   ;; [:.stretch (merge flex/grow
   ;;                   {:width "100%"})]
   [:.spell {:padding "4px 0"}
    [:.header flex]
    [:.meta metadata]
    [:.info (merge flex/grow
                   button) ]
    [:.prepare (merge button
                      text-center
                      {:min-width "5em"})
     [:&.disabled {:cursor 'default
                   :color "#999"}]]]]

  [:.abilities
   [:.ability (merge flex
                     flex/align-center
                     button
                     {:height "1.7em"})
    [:&.buffed
     [:.score {:color "#0d0"}]
     [:.mod {:color "#0d0"}]]
    [:&.nerfed
     [:.score {:color "#d00"}]
     [:.mod {:color "#d00"}]]
    [:.score {:font-size "1.1em"
              :width "1.9em"} ]
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
   [:.filters (merge flex
                     {:border-bottom "1px solid #333"
                      :margin-bottom "8px"})
    [:.filter {:padding "4px"}]]
   [:.attack flex/center
    [:.name flex/grow]
    [:.info-group (merge flex/center
                         flex/vertical-center
                         {:padding "4px"})
     [:.label {:font-size "60%"}]
     [:.dmg.alt {:font-size "65%"}]]]

   [:.spells (merge flex/center
                    {:margin-bottom "8px"})
    [:.section-label {:font-size "90%"
                      :font-weight 'bold}]
    [:.spell-name {:font-size "80%"
                   :font-style 'italic
                   :padding "4px"}]]
   [:.action
    [:.name {:font-size "90%"
             :font-weight 'bold
             :font-style 'italic}]
    [:.consumable (merge flex/center
                         {:font-size "80%"
                          :margin-bottom 0
                          :padding "8px 8px 0 8px"})
     [:.uses {:padding "4px"}]]
    [:.desc metadata]]]

  [:.features-section
   [:.feature
    [:.chosen (merge metadata
                     {:overflow 'hidden
                      :text-overflow 'ellipsis
                      :white-space 'nowrap
                      })]]
   [:.desc metadata]
   [:.chosen-details {:padding-bottom "8px"
                      :align-self 'flex-start}
    [:h5 {:margin 0}]]]

  [:.limited-use-section
   [:.rests flex/center
    [:.button (merge
                flex/grow
                button
                text-center)]]
   [:.limited-use (merge
                    flex/center
                    {:padding "4px"})
    [:.info flex/grow
     [:.recovery metadata]]
    [:.usage
     [:.button (merge button)
      [:&.selected {:background-color "#ddd"}
       [:&:hover {:background-color "#eee"}]]]

     [:.many flex/center
      [:.modify {:padding "8px"}]]]]]

  [:.spells-section
   [:.spell-slot-level flex/center
    [:.label flex/grow]]

   [:.manage-link {:font-weight 'normal
                   :font-size "80%"}]

   [:.spell flex/center
    [:.meta metadata]
    [:.spell-info flex/grow
     [:.name {:font-weight "bold"}]]
    [:.dice (merge text-center
                   {:align-self 'center})]]]

  [:.inventory-section
   [:.special {:justify-content 'space-between}]
   [:.item-browser {:height "300px"
                    :padding "4px"
                    :margin "8px 0 4em 0"}]
   [:.item flex/center
    [:.info flex/grow
     [:.notes-preview {:overflow 'hidden
                       :font-size "10px"
                       :height "12px"
                       :max-width "150px"
                       :padding-right "1em"
                       :text-overflow 'ellipsis
                       :white-space 'pre}]]
    [:.button {:font-size "60%"}
     [:&.disabled {:font-style 'italic
                   :color "rgba(1,1,1, 0.25) !important"
                   :cursor 'default}]
     [:&:hover {:background-color "#f0f0f0"
                :color "#333"}
      [:&.disabled {:background-color "#ccc"}]]]]
   [:.item-info {:padding "0 12px"
                 :font-size "90%"
                 :text-align 'justify}
    [:div.quantity (merge flex/center
                          flex/justify-center)
     [:input.quantity {:font-size "120%"
                       :width "3em"
                       :text-align 'center}]]
    [:.delete (merge flex/center
                     flex/justify-center
                     {:align-self 'center
                      :margin "12px 0"})]]]

  [:.spell-card {:max-width "300px"}
   [:table.info metadata
    [:th.header {:text-align 'right}]]
   [:.desc {:font-size "90%"}]]

  [:.currency-manager-overlay base-overlay
   [:.meta (merge metadata
                  text-center
                  {:max-width "180px"})]
   [:th.header {:font-size "80%"}
    [:&.p {:background-color color-platinum}]
    [:&.g {:background-color color-gold}]
    [:&.e {:background-color color-electrum}]
    [:&.s {:background-color color-silver}]
    [:&.c {:background-color color-copper}]]
   [:.amount {:width "4em"
              :text-align 'center}]]

  [:.currency-preview (merge flex/center
                             {:font-size "10pt"})
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
     [:&.c {:background-color color-copper}]]] ]

  [:.info-overlay overlay
   [:table.info (merge metadata
                       {:margin-top "8px"})
    [:th.header {:text-align 'right}]]
   [:.desc metadata]]

  [:.starting-equipment-overlay overlay
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
              :font-size "120%"}]])

