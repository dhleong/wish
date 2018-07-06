(ns ^{:author "Daniel Leong"
      :doc "multi-limited-select reagent forms widget"}
  wish.views.widgets.multi-limited-select
  (:require-macros [reagent-forms.macros :refer [render-element]])
  (:require [reagent-forms.core :as forms]))

; Much of this code is based off code from reagent-forms, licensed
; under the Eclipse Public License and (c) 2018 Dimitri Sotnikov

;; Based on :list with some extras. Supports an option with special key
;;  :-none which means that no value was selected
(defmethod forms/init-field :multi-limited-select
  [[type {:keys [id] :as attrs} & options] {:keys [doc get save!]}]
  (let [options        (forms/extract-selectors options)
        options-lookup (forms/map-options options)
        selection      (atom (or
                               (get id)
                               (get-in (first options) [1 :key])))]
    ;; (save! id @selection) ; NOTE don't save right away
    (render-element attrs doc
                    [type
                     (merge
                       attrs
                       {:default-value (forms/default-selection options @selection)
                        :on-change     #(let [v (clojure.core/get
                                                  options-lookup
                                                  (forms/value-of %))]
                                          (save! id (when-not (= :-none v)
                                                      v)))})
                     (doall
                       (filter
                         #(if-let [visible (:visible? (second %))]
                            (forms/call-attr doc visible) true)
                         options))])))
