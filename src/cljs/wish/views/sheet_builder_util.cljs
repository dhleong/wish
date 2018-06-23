(ns ^{:author "Daniel Leong"
      :doc "Utility widgets, etc. for implementing a sheet builder"}
  wish.views.sheet-builder-util
  (:require [wish.util :refer [<sub]]
            [wish.util.nav :refer [sheet-url]]
            [wish.views.widgets :refer [link save-state]]))

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
     [:div.sections
      (for [[id info] sections]
        ^{:key id}
        [:div.section-link
         {:class (when (= id current-section)
                   "selected")}
         [link {:href (sheet-url sheet-id :builder id)}
          (:name info)]])
      [save-state]]

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
