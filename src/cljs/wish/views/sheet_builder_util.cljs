(ns ^{:author "Daniel Leong"
      :doc "Utility widgets, etc. for implementing a sheet builder"}
  wish.views.sheet-builder-util
  (:require [reagent.core :as r]
            [wish.util :refer [<sub >evt]]
            [wish.util.nav :refer [sheet-url]]
            [wish.providers :as providers]
            [wish.views.widgets :refer [link save-state] :refer-macros [icon]]))

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
    [:div.builder
     [:div.header.sections
      (for [[id info] sections]
        ^{:key id}
        [:div.section-link
         {:class (when (= id current-section)
                   "selected")}
         [link {:href (sheet-url sheet-id :builder id)}
          (:name info)]])

      [save-state]

      [link {:href (sheet-url sheet-id)}
       (icon :description)]

      ]

     [:div.sections
      [:div.section-arrow.prev
       (when-let [[prev-id prev-info] prev-sec]
         [link {:href (sheet-url sheet-id :builder prev-id)}
          (:name prev-info)])]

      [:div.builder-main
       (if section-info
         [(:fn section-info)]
         [:div.error "Unknown section " current-section])]

      [:div.section-arrow.next
       (if-let [[next-id next-info] next-sec]
         [link {:href (sheet-url sheet-id :builder next-id)}
          (:name next-info)]

         ; TODO probably, only show if the sheet is "ready" to use
         [link {:href (sheet-url sheet-id)}
          "Let's play!"])]]]))

(defn data-source-manager []
  (let [original-ids (<sub [:active-sheet-source-ids])
        selected-source-ids (r/atom original-ids)]
    (fn -data-source-manager []
      (>evt [:query-data-sources])
      (let [sheet-id (<sub [:active-sheet-id])
            this-source-ids @selected-source-ids]
        [:div
         [:h3 "Data Sources"]
         [:div
          (if-let [sources (<sub [:data-sources])]
            (for [s sources]
              (let [{:keys [id]} s]
                ^{:key id}
                [:div
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
                          :checked (contains? this-source-ids id)}]
                 [:label {:for id}
                  (:name s)]]))

            [:<> "No data sources available"])
          (when-not (= original-ids this-source-ids)
            [:<>
             (when-not (some #(= "wish" (namespace %)) this-source-ids)
               [:p.warning
                "Warning! Removing the builtin data source may cause problems!"])
             [:input {:type 'button
                      :on-click (fn [e]
                                  (.preventDefault e)
                                  (>evt [:set-sheet-data-sources
                                         sheet-id
                                         this-source-ids]))
                      :value "Save Changes"}]])]

         [:h5 "Add New Data Source on:"]
         [:div

          (for [[provider-id state] (<sub [:provider-states])]
            ^{:key provider-id}
            [:div
             [:a {:href "#"
                  :on-click (fn [e]
                              (.preventDefault e)
                              (providers/register-data-source provider-id))}
              (:name (providers/get-info provider-id))]
             (when (= state :signed-out)
               [link {:href (str "/providers/" (name provider-id) "/config")}
                "Configure"])])]]))))
