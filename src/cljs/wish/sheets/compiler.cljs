(ns wish.sheets.compiler
  (:require [wish-engine.model :as engine-model]))

(defn- compile-sheet-items [{:keys [engine]} items]
  (reduce-kv
    (fn [m id item]
      (if-let [apply-fn (:! item)]
        (assoc-in m [id :!]
                  (engine-model/eval-source-form @engine nil apply-fn))
        m))
    items
    items))

(defn compile-sheet [kind-meta sheet]
  (-> sheet
      (update :items (partial compile-sheet-items kind-meta))))
