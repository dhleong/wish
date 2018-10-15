(ns ^{:author "Daniel Leong"
      :doc "limited-select"}
  wish.views.widgets.limited-select
  (:require-macros [wish.util :refer [fn-click]])
  (:require [wish.views.widgets.virtual-list :refer [virtual-list]]))

(defn toggle-option-set
  "Return the a set after toggling the presence of `value`.
   If `single-select?`, providing a value that doesn't exist
   in `old-set` will deselect all other values."
  [old-set single-select? value]
  (cond
    (contains? old-set value)
    (disj old-set value)

    ; change single-select option
    single-select?
    #{value}

    :else
    (conj old-set value)))

(defn toggle-option
  "Given a set of IDs, some extra info, and an option ID,
   return the value vector representing the result of attempting
   to toggle the given option-id."
  [old-set accepted? extra-info single-select? option-id]
  (let [new-v (->> (toggle-option-set old-set single-select? option-id)
                   (into []))]
    (if (accepted? (assoc
                     extra-info
                     :features new-v))
      new-v

      ; no change
      (into [] old-set))))

(defn limited-select
  [& {:keys [accepted?
             doc
             extra-info
             options
             path
             render-item]}]
  {:pre [(identity accepted?)
         (identity doc)
         (identity options)
         (identity path)
         (identity render-item)]}
  (let [{doc-get :get
         doc-save! :save!} doc

        selected (set (doc-get path))
        total-items (count options)
        scrollable? (>= total-items 15)
        single-select? (= 1 (-> accepted? meta :const))

        do-toggle-option (fn [option-id]
                           (doc-save!
                             path
                             (toggle-option
                               selected
                               accepted? extra-info single-select?
                               option-id)))

        do-render-item (fn [option]
                         (let [active? (contains? selected (:id option))]
                           (render-item
                             {:class (when active? "active")
                              :active? active?
                              :on-click (fn-click
                                          (do-toggle-option (:id option)))}
                             option)))]

    [:div.feature-options {:class (when scrollable?
                                    "scrollable")}
     (if scrollable?
       [virtual-list
        :items options
        :render-item do-render-item]

       (for [option options]
         (with-meta
           (do-render-item option)
           {:key (:id option)})))]))
