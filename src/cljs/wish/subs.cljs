(ns wish.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]))

(reg-sub :page :page)

(reg-sub
 ::re-pressed-example
 (fn [db _]
   (:re-pressed-example db)))
