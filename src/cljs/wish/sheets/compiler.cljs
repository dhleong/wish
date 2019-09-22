(ns wish.sheets.compiler
  (:require [wish-engine.model :as engine-model]))

(defn- compile-sheet-items [{:keys [engine]} items]
  (reduce-kv
    (fn [m id item]
      (if-let [apply-fn (:! item)]
        (-> m
            (assoc-in [id :!]
                      (engine-model/eval-source-form @engine nil apply-fn))
            (assoc-in [id :!-raw] apply-fn))
        m))
    items
    items))

(defn compile-sheet [kind-meta sheet]
  (-> sheet
      (update :items (partial compile-sheet-items kind-meta))))


; ======= decompile =======================================

(defn- decompile-sheet-items [items]
  (reduce-kv
    (fn [m id item]
      (if (:!-raw item)
        (-> m
            (assoc-in [id :!] (:!-raw item))
            (update id dissoc :!-raw))
        m))
    items
    items))

(defn decompile-sheet [_kind-meta sheet]
  (-> sheet
      (update :items decompile-sheet-items)))
