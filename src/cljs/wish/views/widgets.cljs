(ns ^{:author "Daniel Leong"
      :doc "Shared widgets"}
  wish.views.widgets
  (:require-macros [wish.views.widgets :refer [icon]])
  (:require [clojure.string :as string]
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
