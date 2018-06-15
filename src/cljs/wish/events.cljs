(ns wish.events
  (:require [re-frame.core :refer [dispatch reg-event-db reg-event-fx
                                   trim-v]]
            [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
            [wish.db :as db]
            [wish.providers :as providers]))

(reg-event-fx
  ::initialize-db
  (fn-traced [_ _]
    {:db db/default-db
     :providers/init! :!}))

(reg-event-db
  :navigate!
  [trim-v]
  (fn-traced [db page-spec]
    (assoc db :page page-spec)))


; ======= sheet-related ====================================

(reg-event-fx
  :load-sheet!
  [trim-v]
  (fn-traced [_ [sheet-id]]
    {:load-sheet! sheet-id}))

; sheet loaded
(reg-event-db
  :put-sheet!
  [trim-v]
  (fn-traced [db [sheet-id sheet]]
    (println "PUT " sheet-id)
    (assoc-in db [:sheets sheet-id] sheet)))

(reg-event-fx
  :load-sheet-source!
  [trim-v]
  (fn-traced [{:keys [db]} [sheet-id sources]]
    ; no dup loads, pls
    (when-not (get-in db [:sheet-sources sheet-id])
      {:db (assoc-in db [:sheet-sources sheet-id] {})
       :load-sheet-source! [sheet-id sources]})))

(reg-event-db
  :put-sheet-source!
  [trim-v]
  (fn-traced [db [sheet-id source]]
    (assoc-in db [:sheet-sources sheet-id]
              {:loaded? true
               :source source})))
