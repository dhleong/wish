(ns ^{:author "Daniel Leong"
      :doc "Shared widgets"}
  wish.views.widgets
  (:require-macros [wish.views.widgets :refer [icon]])
  (:require [clojure.string :as string]
            [reagent.core :as r]
            [wish.util :refer [<sub >evt click>evt]]
            [wish.util.formatted :refer [->hiccup]]
            [wish.util.nav :refer [pushy-supported?]]))

(defn formatted-text
  [container-spec text]
  (vec
    (cons
      container-spec
      (map
        (fn [p]
          [:p p])
        (->hiccup text)))))

(defn link
  "Drop-in replacement for :a that inserts the # in links if necessary"
  [attrs & contents]
  (if pushy-supported?
    (vec (concat [:a attrs] contents))
    (vec (concat [:a (update attrs
                             :href
                             (fn [s]
                               (str "#" s)))]
                 contents))))

(defn save-state
  []
  (let [save-state (<sub [:save-state])]
    [:div.save-state
     (case save-state
       :idle (icon :cloud-done)
       :pending (icon :cloud-queue)
       :saving (icon :cloud-upload))]))

(defn expandable
  "Helper widget to render an expandable cell. When the header
   is clicked, it will expand and contract. The header-fn is
   called with a bool indicating whether it's currently expanded,
   in case you want to change rendering based on that.
   header-fn and expanded-fn may also just be forms"
  [header-fn expanded-fn & [{:keys [start-expanded?]}]]
  (let [expanded? (r/atom start-expanded?)]
    (fn [header-fn expanded-fn]
      (let [now-expanded? @expanded?]
        [:div.expandable
         [:div.header.button
          {:on-click (fn [e]
                       (.preventDefault e)
                       (swap! expanded? not))}
          (if (fn? header-fn)
            [header-fn now-expanded?]
            header-fn)]

         (when now-expanded?
           [:div.content
            (if (fn? expanded-fn)
              [expanded-fn]
              expanded-fn)])]))))
