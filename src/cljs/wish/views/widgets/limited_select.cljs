(ns ^{:author "Daniel Leong"
      :doc ":limited-select widget for react-forms"}
  wish.views.widgets.limited-select
  (:require-macros [reagent-forms.macros :refer [render-element]])
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [reagent-forms.core :as forms]))

; Much of this code is based off code from reagent-forms, licensed
; under the Eclipse Public License and (c) 2018 Dimitri Sotnikov

(defn selections->options
  "Convert a selections map into an options vector"
  [selections]
  (->> selections (filter second) (mapv first)))

(defn update-selections-for
  [selections accepted? accepted?-extra key-clicked]
  (let [const-max (-> accepted? meta :const)]
    (if (= 1 const-max)
      ; act like a radio button
      {key-clicked true}

      (let [updated (update-in selections [key-clicked] not)]
        ; TODO handling filters, esp. for spells?
        (if (accepted? (merge
                         (when accepted?-extra
                           @accepted?-extra)
                         {:features (selections->options updated)}))
          ; only return the updated value if it was accepted
          updated

          ; no deal; don't change
          selections)))))

(defn- clean-attrs
  [attrs]
  (-> attrs
      forms/clean-attrs
      (dissoc :accepted? :accepted?-extra
              :visible?!)))

(defn- group-item
  [[type {:keys [key touch-event disabled] :as attrs} & body]
   {:keys [save! accepted? accepted?-extra]} selections field id]
  (letfn [(handle-click! []
            (let [old-val @selections
                  new-val (swap! selections
                                 update-selections-for
                                 accepted?
                                 accepted?-extra
                                 key)]
              (when (not= old-val new-val)
                (save! id (selections->options new-val)))))]
    (fn group-item-renderer []
      (let [disabled?        (if (fn? disabled) (disabled) disabled)
            active?          (get @selections key)
            ; NOTE we never use this widget for button or input, and
            ; dropping support lets us know in the widget whether or not
            ; we're currently selected
            button-or-input? false #_(let [t (subs (name type) 0 5)]
                               (or (= t "butto") (= t "input")))
            class            (->> [(when active? "active")
                                   (when (and disabled? (not button-or-input?)) "disabled")]
                                  (remove str/blank?)
                                  (str/join " "))]
        [type
         (dissoc
           (merge {:class class
                   :active? active?
                   (or touch-event :on-click)
                          (when-not disabled? handle-click!)}
                  (clean-attrs attrs)
                  {:disabled disabled?})
           (when-not button-or-input? :disabled))
         body]))))

(defn- mk-selections [id selectors {:keys [get multi-select] :as ks}]
  (let [value (get id)]
    (reduce
      (fn [m [_ {:keys [key]}]]
        (assoc m key (boolean (some #{key} value))))
      {} selectors)))

(defn- selection-group
  [[type {:keys [field id] :as attrs} & selection-items] {:keys [get doc] :as opts}]
  (let [selection-items (forms/extract-selectors selection-items)
        selections      (r/atom (mk-selections id selection-items opts))
        selectors       (map (fn [item]
                               ; I'm not sure why :visible? gets stripped
                               ; before we get here, but it does...
                               {:visible? (or (:visible? (second item))
                                              (:visible?! (second item)))
                                :selector [(group-item item opts selections field id)]})
                             selection-items)]
    (fn []
      (when-not (get id)
        (swap! selections #(into {} (map (fn [[k]] [k false]) %))))
      (into [type (clean-attrs attrs)]
            (->> selectors
                 (filter
                   #(if-let [visible? (:visible? %)]
                      (forms/call-attr doc visible?)
                      true))
                 (map :selector))))))


; NOTE: if provided, accepted?-extra must be an ATOM. This is thanks
; to how reagent-forms disregards any changed inputs on subsequent
; renders :\
(defmethod forms/init-field :limited-select
  [[_ {:keys [accepted? accepted?-extra] :as attrs} :as field] {:keys [doc] :as opts}]
  (render-element attrs doc
                  [selection-group field (assoc opts
                                                :accepted? accepted?
                                                :accepted?-extra accepted?-extra)]))
