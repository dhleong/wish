(ns wish.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]))

(defn reg-sheet-sub
  [name getter]
  (reg-sub
    name
    :<- [:sheet]
    (fn [[sheet]]
      (getter sheet))))

(reg-sub :page :page)
(reg-sub :sheet :sheet)
(reg-sheet-sub :sheet-data :sheet)
(reg-sheet-sub :classes :classes)
(reg-sheet-sub :races :races)
(reg-sheet-sub :race (comp first :races)) ;; semantic convenience for single-race systems
(reg-sheet-sub :limited-uses :limited-uses)
(reg-sheet-sub :options :options)

(reg-sub
  :provided-sheets
  (fn [db [_ sheet-id]]
    (get-in db [:sheets sheet-id])))

(reg-sub
 ::re-pressed-example
 (fn [db _]
   (:re-pressed-example db)))
