(ns wish.sheets.compiler
  (:require [wish-engine.model :as engine-model]))

(defn sheet-items [engine items]
  (reduce-kv
    (fn [m id item]
      (if-let [apply-fn (:! item)]
        (-> m
            (assoc-in [id :!]
                      (engine-model/eval-source-form engine nil apply-fn))
            (assoc-in [id :!-raw] apply-fn))
        m))
    items
    items))
