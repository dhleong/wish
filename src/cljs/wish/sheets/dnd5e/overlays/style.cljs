(ns wish.sheets.dnd5e.overlays.style
  (:require [spade.core :refer [defattrs]]
            [wish.style :as theme]
            [wish.style.flex :as flex :refer [flex]]
            [wish.style.media :as media]
            [wish.style.shared :refer [metadata]]
            [wish.sheets.dnd5e.style :as styles]))

(def base-overlay {:padding "32px"})
(def overlay (merge base-overlay
                    {:width "300px"}))

(defattrs ability-tmp-overlay []
  overlay

  [:.number {:font-size "110%"
             :margin-left "8px"
             :width "3em"}])

(defattrs currency-manager-overlay []
  (at-media media/tiny
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
  [:table (merge styles/text-center
                 flex/vertical
                 flex/align-center
                 {:border-collapse 'collapse})]
  [:th.header {:font-size "80%"}
   (at-media media/dark-scheme
     {:color theme/text-primary-on-light})
   [:&.p {:background-color styles/color-platinum} ]
   [:&.g {:background-color styles/color-gold}]
   [:&.e {:background-color styles/color-electrum}]
   [:&.s {:background-color styles/color-silver}]
   [:&.c {:background-color styles/color-copper}]]
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
  (at-media media/smartphones
            [:.quick-adjust :.new-hp
             {:width "15vw !important"}]
            [:.quick-adjust (merge flex/vertical
                                   flex/align-center)
             [:.number {:width "15vw !important"}]
             [:.label {:font-size "80%"
                       :overflow-x 'visible
                       :white-space 'nowrap}]])
  overlay

  [:.current-hp (merge styles/text-center
                       {:width "5em"
                        :font-size "1.2em"})
   [:&.buffed {:color "#00cc00"}]
   [:&.nerfed {:color "#cc0000"}]]
  [:.centered styles/text-center]
  [:.section-header {:margin-bottom "0px"}]

  [:.new-hp (merge styles/text-center
                   {:padding "12px"
                    :width "4em"})
   [:.label {:font-size "80%"}]
   [:.amount {:font-size "140%"}
    [:&.healing {:color "#00cc00"}]
    [:&.damage {:color "#cc0000"}]]
   [:input.apply (merge
                   styles/button
                   {:margin-top "1em"})]]
  [:.quick-adjust (merge styles/text-center
                         {:padding "4px"})
   [:.number (merge styles/text-center
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
  (at-media media/smartphones
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
    [:&.disabled styles/disabled-button]
    [:&:hover {:background-color "#f0f0f0"
               :color "#333"}
     [:&.disabled {:background-color "#ccc"}]]]] )

(defattrs notes-overlay []
  (at-media media/smartphones
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
    [:.hit-die styles/button]]
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
               :background styles/color-accent-nerf
               :content "''"
               :display 'block}]]
  [:.spell {:padding "4px 0"}
   [:.header flex]
   [:.meta metadata]
   [:.info (merge flex/grow
                  styles/button)]
   [:.prepare (merge styles/button
                     styles/text-center
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

    (at-media media/dark-scheme
      {:color [["rgba(255,255,255, 0.5)"]]})

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
                :left "3em"}
      (at-media media/dark-scheme
        {:background-color "#444"})]]
    [:&.chosen {:color "#000"}
     (at-media media/dark-scheme
       {:color theme/text-primary-on-dark})]

    ]]
  [:.pack
   [:.contents metadata]]
  [:.accept {:padding "8px"
             :font-size "120%"}])
