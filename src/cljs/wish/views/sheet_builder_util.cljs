(ns ^{:author "Daniel Leong"
      :doc "Utility widgets, etc. for implementing a sheet builder"}
  wish.views.sheet-builder-util
  (:require-macros [wish.util :refer [fn-click]])
  (:require [reagent.core :as r]
            [spade.core :refer [defattrs defclass]]
            [wish.util :refer [<sub >evt click>evt]]
            [wish.util.nav :refer [sheet-url]]
            [wish.providers :as providers]
            [wish.style.media :as media]
            [wish.style.util :refer [linear-gradient]]
            [wish.views.widgets :refer [link link>evt save-state] :refer-macros [icon]]))

(defclass sections-class []
  {:display 'flex
   :flex-wrap [['wrap :!important]]
   :justify-content 'center}

  [:&.spread {:width "100%"
              :justify-content 'space-between}]
  [:.section {:width "400px"
              :margin "8px"}])

; ======= util ============================================

(defn count-max-options
  ([feature] (count-max-options feature nil))
  ([{values :values
     accepted? :max-options}
    extra-info]
   ; if it's a const number, we can skip some steps
   (or (when (number? accepted?)
         accepted?)

       (first
         (keep
           (fn [to-take]
             (when-not (accepted? (merge
                                    extra-info
                                    {:features
                                     (take to-take values)}))
               (dec to-take)))
           (range 1 (inc (count values)))))

       ;; fallback, I guess?
       (count values))))


; ======= builder section routing =========================

(defattrs builder-attrs []
  [:.header {:display 'flex
             :flex-direction 'column
             :align-items 'center
             :padding-bottom "1em"}
   [:&.sticky {:position 'sticky
               :top 0
               :background (linear-gradient
                             :180deg
                             ["#fff" :50%]
                             ["rgba(255,255,255,0)" :100%])}
    (at-media media/dark-scheme
     {:background (linear-gradient
                    :180deg
                    ["#000" :50%]
                    ["rgba(0,0,0,0)" :100%])})]]

  [:.builder-main {:width "600px"}]
  [:.section-arrow {:padding "4px 8px"}
   [:&.prev {:margin-right "8px"}]
   [:&.empty {:visibility 'hidden}]])

(defattrs header-row [& sticky?]
  {:composes (sections-class)
   :align-items 'center

   :position (when sticky?
               'sticky)
   :top 0}

  [:.section-link
   [:&.selected [:a {:color [["#000" :!important]]}
                 (at-media media/dark-scheme
                   {:color [["#fff" :!important]]})]]

   [:.nav-link {:display 'flex
                :align-items 'center}]

   (at-media media/smartphones
     [:.label {:display 'none}])
   ])

(defn- find-section
  [candidates target-id]
  (reduce-kv
    (fn [_ i [id info]]
      (when (= id target-id)
        (reduced
          [i id info])))
    nil
    candidates))


(defn router
  "Sections should be a vector of [id {:name, :fn}] pairs,
   in the order in which they should appear"
  [sections current-section]
  (let [sheet-id (<sub [:active-sheet-id])
        [index _ section-info] (find-section sections current-section)
        prev-sec (when (> index 0)
                   (nth sections (dec index)))
        next-sec (when (< index (dec (count sections)))
                   (nth sections (inc index)))]
    (println "[builder-router] " sheet-id " / " current-section)
    [:div (builder-attrs)
     [:div.header
      [:div (header-row)
       (for [[id info] sections]
         ^{:key id}
         [:div.section-link
          {:class (when (= id current-section)
                    "selected")}
          [link {:href (sheet-url sheet-id :builder id)}
           [:span.icon (:icon info)]
           [:span.label (:name info)]]])]]

     [:div.sticky.header
      [:div (header-row)
       [:div.section-arrow.prev {:class (when-not prev-sec
                                          :empty)}
        (if-let [[prev-id _] prev-sec]
          [link {:href (sheet-url sheet-id :builder prev-id)}
           (icon :arrow-back)]

          [:div.nav-link
           (icon :arrow-back)])]

       [save-state]

       [link {:href (sheet-url sheet-id)}
        (icon :description)]

       [:div.section-arrow.next
        (if-let [[next-id _] next-sec]
          [link {:href (sheet-url sheet-id :builder next-id)}
           (icon :arrow-forward)]
          [link {:href (sheet-url sheet-id)}
           (icon :check)])]
       ]]

     [:div {:class (sections-class)}
      [:div.builder-main
       (if section-info
         [(:fn section-info)]
         [:div.error "Unknown section " current-section])]
      ]]))


; ======= campaign management =============================

(defn campaign-manager []
  (when-let [campaign-info (<sub [:meta/campaign])]
    (r/with-let [wants-to-leave? (r/atom false)]
      [:<>
       [:h3 "Campaign"]

       [:div.group
        (<sub [:meta/name])
        " is currently part of "
        (or [:b (:name campaign-info)]
            "a campaign")
        "."]

       (if @wants-to-leave?
         [:div.warning
          [:div.group
           "This does NOT revoke your DM's access to this sheet. You will have to do that via the share menu. "
           (when-let [sheet-id (<sub [:sharable-sheet-id])]
             [:<>
              [link>evt [:share-sheet! sheet-id]
               "Click here"]
              " to do this now."]) ]

          [:div.group
           [:input {:type 'button
                    :on-click (click>evt [:join-campaign])
                    :value "I understand; leave the campaign"}]]]

         [:div.group
          [link>evt {:on-click (fn-click
                                 (swap! wants-to-leave? not))}
           "Click here if you want to leave this campaign."]
          ])])))


; ======= data source management ==========================

(defattrs refresh-button-attrs []
  {:display 'inline}
  [:i.material-icons {:font-size "16pt"
                      :vertical-align 'middle}]
  [:.invisible {:visibility 'hidden}])

(defn- data-source [selected-source-ids {:keys [id] :as s}]
  (let [sheet-id (<sub [:active-sheet-id])
        selected? (contains? @selected-source-ids id)]
    [:div
     [:div (refresh-button-attrs)
      (if selected?
        [link>evt [:reload-sheet-source! sheet-id id]
         (icon :refresh)]

        ; for spacing:
        (icon :refresh.invisible))]

     [:input {:id id
              :name 'sources
              :type 'checkbox
              :on-change (fn [e]
                           (let [m (if (.. e -target -checked)
                                     conj
                                     disj)]
                             (swap! selected-source-ids
                                    m
                                    id)))
              :checked selected?}]
     [:label {:for id}
      (:name s)]]))

(defn data-source-manager []
  (r/with-let [original-ids (<sub [:active-sheet-source-ids])
               selected-source-ids (r/atom original-ids)]
    (>evt [:query-data-sources])
    (let [sheet-id (<sub [:active-sheet-id])
          this-source-ids @selected-source-ids]
      [:<>
       [:h3 "Data Sources"]
       [:div
        (if-let [sources (<sub [:data-sources])]
          (for [{:keys [id] :as s} sources]
            ^{:key id}
            [data-source selected-source-ids s])

          [:<> "No data sources available"])

        (when-not (= original-ids this-source-ids)
          [:<>
           (when-not (some #(= "wish" (namespace %)) this-source-ids)
             [:p.warning
              "Warning! Removing the builtin data source may cause problems!"])
           [:input {:type 'button
                    :on-click (click>evt [:set-sheet-data-sources
                                          sheet-id
                                          this-source-ids])
                    :value "Save Changes"}]])]

       [:h5 "Add New Data Source on:"]
       [:div

        (for [[provider-id state] (<sub [:provider-states])]
          ^{:key provider-id}
          [:div
           [:a {:href "#"
                :on-click (fn-click
                            (providers/register-data-source provider-id))}
            (:name (providers/get-info provider-id))]
           (when (not= state :ready)
             [link {:href (str "/providers/" (name provider-id) "/config")}
              "Configure"])])]])))
