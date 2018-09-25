(ns ^{:author "Daniel Leong"
      :doc "dnd5e.style"}
  wish.sheets.dnd5e.style
  (:require [garden.color :as color]
            [wish.style :refer [defclass defstyled]]
            [wish.style.flex :as flex :refer [flex]]
            [wish.style.shared :refer [metadata]]))

(def color-accent "#fbc02d")
(def color-accent2 "#6f49b9")

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

(defn proficiency-style [& {:as extra}]
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
     :border (str "1px solid " color-proficient)
     :background color-proficient
     :display 'inline-block
     :position 'absolute
     :visibility 'hidden}]
   [:&.proficient::before
    {:visibility 'visible}]
   [:&.expert::before
    {:background-color color-expert
     :border-color color-expert}]

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
     :border (str "1px solid " color-proficient)
     :border-radius "50%"
     :display 'inline-block
     :position 'absolute
     }] ])

;;
;; 35/65 layout
;;

(defstyled container
  (merge flex/vertical
         {:height "100%"}))

(defstyled layout
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
;; The header bar
;;

(defstyled header-container
  {:display 'block
   :background "#666666"})

(defstyled header
  (at-media
    media-tablets
    [:.col.meta {:max-width "15vw"}])

  (at-media
    media-smartphones
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

  (at-media
    media-tiny
    {:font-size "80%"}
    [:.side {:padding "0 !important"}])

  [:& (merge flex
             flex/wrap
             {:color "#f0f0f0"
              :margin "0 auto"
              :padding "4px 0"
              :max-width "1200px"
              :width "100%"})]
  [:.side flex
   [:&.left {:padding-left "12px"}]
   [:&.settings {:order 1
                 :padding-right "12px"}]

   [:.col (merge flex/vertical-center
                 text-center
                 {:padding "4px 8px"})
    [:&.left {:text-align 'left}]

    [:.meta (merge flex
                   flex/wrap
                   {:font-size "80%"})
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

  [:.space flex/grow])

;;
;; Overlays
;;

(defstyled ability-tmp-overlay
  overlay

  [:.number {:font-size "110%"
             :margin-left "8px"
             :width "3em"}])

(defstyled currency-manager-overlay
  base-overlay

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
             :text-align 'center}])

(defstyled custom-item-overlay
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

(defstyled hp-overlay
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

(defstyled info-overlay
  overlay

  [:table.info (merge metadata
                      {:margin-top "8px"})
   [:th.header {:text-align 'right}]]
  [:.desc metadata])

(defstyled item-adder-overlay
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
    [:&.disabled {:font-style 'italic
                  :color "rgba(1,1,1, 0.25) !important"
                  :cursor 'default}]
    [:&:hover {:background-color "#f0f0f0"
               :color "#333"}
     [:&.disabled {:background-color "#ccc"}]]]] )

(defstyled notes-overlay
  (at-media
    media-smartphones
    [:textarea.notes {:height "80vh"
                      :font-size "10pt !important"}])

  (merge base-overlay
         {:width "80vw"})

  [:textarea.notes {:width "100%"
                    :font-size "12pt"
                    :min-height "50vh"}])

(defstyled short-rest-overlay
  (merge base-overlay
         {:max-width "400px"})

  [:.sections {:margin-bottom "1em"
               :justify-content 'start}
   [:.hit-dice-pool {:margin-right "2em"}
    [:.hit-die button]]
   [:.hit-die-use flex/center
    [:.hit-die-value {:width "5em"}]]]
  [:.desc metadata])

(defstyled spell-info-overlay base-overlay)

(defstyled spell-management-overlay
  base-overlay

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
                  :color "#999"}]]])

(defstyled starting-equipment-overlay
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

(defclass currency-preview
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

(defstyled inventory-quantity
  (merge flex/center
         flex/justify-center)

  [:input.quantity {:font-size "120%"
                    :width "3em"
                    :text-align 'center}])

(defclass cast-spell
  (merge text-center
         {:width "3.5em"
          :padding "4px"
          :margin "0 8px 0 0"})
  [:&.upcast {:position 'relative
              :border (str "2px solid " color-accent2)}
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

  [:&.button
   [:&.disabled {:font-style 'italic
                 :color "rgba(1,1,1, 0.25) !important"
                 :cursor 'default} ]
   [:&:hover {:background-color "#f0f0f0"
              :color "#333"}
    [:&.disabled {:background-color "#ccc"}]]])

(defstyled spell-card
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

(defstyled spell-tags
  [:.tag {:margin "0 4px"
          :padding "0 4px"
          :color "#fff"
          :font-size "80%"
          :border-radius "12px"
          :background-color "#333"}])

; in dnd5e.cljs, not widgets:
(defstyled rest-buttons
  (merge flex/center
         {:margin "8px 0"})

  [:.button (merge
              flex/grow
              button
              text-center)])

(defstyled swipeable-page
  {:min-height "60vh"})

;;
;; Sections
;;

(defstyled abilities-section
  ; make the mod a bit more prominent if we have room
  (at-media
    {:min-width "1000px"}
    [:.abilities>.ability>.mod {:font-size "2em"}])

  {:margin-top "1em"}

  [:.abilities (merge flex
                      {:justify-content 'space-around})]

  [:&>.info (merge metadata
                   text-center)]

  [:.ability (merge flex/vertical
                    flex/center
                    flex/align-center
                    text-center
                    button)
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

(defstyled actions-section
  [:.filters (merge flex
                    {:border-bottom "1px solid #333"
                     :margin-bottom "8px"
                     :overflow-x 'auto})
   [:.filter {:padding "4px"}]]

  [:.combat-info (merge metadata
                        {:margin-bottom "8px"})
   [:.item
    ["&:not(:first-child)" {:padding-right "0.5em"}
     [:&:before {:content "'·'"
                 :display 'inline-block
                 :text-align 'center
                 :width "1em"}]]]]

  [:.attack flex/center
   [:.name flex/grow]
   [:.info-group (merge flex/center
                        flex/vertical-center
                        {:padding "4px"})
    [:.label {:font-size "60%"}]
    [:.dmg.alt {:font-size "65%"}]]]
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
   [:.consumable (merge flex/center
                        {:font-size "80%"
                         :margin-bottom 0
                         :padding "8px 8px 0 8px"})
    [:.uses {:padding "4px"}]]
   [:.desc metadata]])

(def single-column-skills [:.base-ability
                           {:width "3em !important"}])

(defstyled skills-section
  ; collapse into a single row on smaller devices
  ; that can't fit two columns of Skills
  (at-media
    (merge media-tablets
           media-not-smartphones)
    single-column-skills)

  (at-media
    {:max-width "370px"}
    single-column-skills)

  (at-media
    (merge media-smartphones
           {:min-width "371px"})
    [:.skill-col {:max-width "48%"}])

  (at-media
    media-laptops
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
    [:.score {:padding "0 4px"}]

    (proficiency-style
      :transform "translate(0, 34%)")]])

(defstyled proficiencies-section
  [:.section {:padding "0 8px"
              :margin-bottom "16px"}]
  [:.item (merge metadata
                 {:display 'inline-block})
   ["&:not(:last-child)" {:padding-right "0.5em"}
    [:&:after {:content "','"}]]])

(defstyled features-section
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

(defstyled limited-use-section
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
     [:.modify {:padding "8px"}]]]])

(defstyled spells-section
  [:.spell-slot-level flex/center
   [:.label flex/grow]]

  [:.manage-link {:font-weight 'normal
                  :font-size "80%"}]

  [:.spell flex/center
   [:.upcast {:color color-accent2}]
   [:.meta metadata]
   [:.spell-info flex/grow
    [:.name {:font-weight "bold"}]]
   [:.dice (merge text-center
                  {:align-self 'center}) ]] )

(defstyled inventory-section
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
    [:&.disabled {:font-style 'italic
                  :color "rgba(1,1,1, 0.25) !important"
                  :cursor 'default}]
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


