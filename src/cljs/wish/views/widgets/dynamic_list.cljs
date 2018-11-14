(ns ^{:author "Daniel Leong"
      :doc "Implementation of `:dynamic-list` reagent-forms element. Like `:list`,
            but the `:content` of the `[:select]` elements is computed by function.
            Note that you MUST provide a unique placeholder content value"}
  wish.views.widgets.dynamic-list
  (:require-macros [reagent-forms.macros :refer [render-element]])
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [reagent-forms.core :as forms]))

; Much of this code is based off code from reagent-forms, licensed
; under the Eclipse Public License and (c) 2018 Dimitri Sotnikov

(defmethod forms/init-field :dynamic-list
  [[type {:keys [id] :as attrs} & options] {:keys [doc get save!]}]
  (let [options        (forms/extract-selectors options)
        options-lookup (atom (forms/map-options options))
        selection      (atom (or
                               (get id)
                               (get-in (first options) [1 :key])))]
    (save! id @selection)
    (render-element attrs doc
                    [type
                     (merge
                       attrs
                       {:default-value (let [v (forms/default-selection options @selection)]
                                         (when (map? v)
                                           (js/console.warn
                                             "No placeholder content provided to dynamic-list " id))
                                         v)
                        :on-change     #(save! id (clojure.core/get @options-lookup (forms/value-of %)))})

                     (->> options
                          (filter #(if-let [visible (:visible? (second %))]
                                     (forms/call-attr doc visible)
                                     true))
                          (map (fn [[option-key opts :as option-vec]]
                                 (if-let [content (:content opts)]
                                   ; dynamic content:
                                   (let [dyn-cont (forms/call-attr doc content)]
                                     ; update options-lookup
                                     (swap! options-lookup
                                            assoc
                                            dyn-cont
                                            (:key opts))
                                     ; dynamic options component
                                     [option-key
                                      (dissoc opts :content)
                                      dyn-cont])

                                   ; static content:
                                   option-vec)))
                          doall)])))
