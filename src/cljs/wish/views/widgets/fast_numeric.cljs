(ns ^{:author "Daniel Leong"
      :doc ":fast-numeric widget for react-forms"}
  wish.views.widgets.fast-numeric
  (:require-macros [reagent-forms.macros :refer [render-element]])
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [reagent-forms.core :as forms]))

(defn- limit-value
  [attrs v]
  (when v
    (let [minv (:min attrs)
          maxv (:max attrs)]
      (if (and minv maxv)
        (max minv
             (min v maxv))

        ; no limits
        v))))

; :fast-numeric is exactly like :numeric, but dispatches (save!)
; on change rather than on blur. It also limits the value range
; if :min and :max are provided as attrs
(defmethod forms/init-field :fast-numeric
  [[type {:keys [id fmt] :as attrs}] {:keys [get save! doc]}]
  (render-element
    attrs doc
    [type (merge
            {:type :number
             :value (get id "")
             :on-change #(->> (forms/value-of %)
                              (forms/format-value fmt)
                              (forms/format-type :numeric)
                              (limit-value attrs)
                              (save! id))}
            attrs)]))

