(ns wish.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]))

(defn reg-sheet-sub
  [name getter]
  (reg-sub
    name
    :<- [:sheet]
    (fn [sheet]
      (getter sheet))))

(reg-sub :page :page)
(reg-sub :sheets :sheets)
(reg-sub :sheet-sources :sheet-sources)

(reg-sheet-sub :sheet-data :sheet)
(reg-sheet-sub :classes :classes)
(reg-sheet-sub :races :races)
(reg-sheet-sub :race (comp first :races)) ;; semantic convenience for single-race systems
(reg-sheet-sub :limited-uses :limited-uses)
(reg-sheet-sub :options :options)

(reg-sub
  :active-sheet-id
  :<- [:page]
  (fn [page-vec _]
    (let [[page args] page-vec]
      (when (= :sheet page)
        ; NOTE: the first arg is the sheet kind;
        ; the second is the id
        (second args)))))

(reg-sub
  :sheet
  :<- [:sheets]
  :<- [:active-sheet-id]
  (fn [[sheets id]]
    (get sheets id)))

(reg-sub
  :provided-sheet
  :<- [:sheets]
  (fn [sheets [_ sheet-id]]
    (get sheets sheet-id)))

(reg-sub
  :sheet-source
  :<- [:sheet-sources]
  (fn [sources [_ sheet-id]]
    (let [{:keys [source loaded?]} (get sources sheet-id)]
      (when loaded?
        source))))

(reg-sub
 ::re-pressed-example
 (fn [db _]
   (:re-pressed-example db)))
