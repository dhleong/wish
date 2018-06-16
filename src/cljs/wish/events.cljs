(ns wish.events
  (:require [re-frame.core :refer [dispatch reg-event-db reg-event-fx
                                   inject-cofx trim-v]]
            [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
            [vimsical.re-frame.cofx.inject :as inject]
            [wish.db :as db]
            [wish.providers :as providers]
            [wish.subs :refer [active-sheet-id]]
            [wish.util :refer [invoke-callable]]))

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

(defn apply-limited-use-trigger
  [limited-used-map limited-uses trigger]
  (reduce
    (fn [m [use-id used]]
      (if-let [use-obj (get limited-uses use-id)]
        (let [restore-amount (invoke-callable
                               use-obj
                               :restore-amount
                               :used used
                               :trigger trigger)
              new-amount (max 0
                              (- used
                                 restore-amount))]
          (assoc m use-id new-amount))

        ; else:
        (do
          (js/console.warn "Have unrelated limited-use " use-id " !!")
          ; TODO should we just dissoc the use-id?
          m)))
    limited-used-map
    limited-used-map))

(reg-event-fx
  :trigger-limited-use-restore
  [trim-v
   (inject-cofx ::inject/sub [:limited-uses])
   (inject-cofx ::inject/sub [:active-sheet-id])]
  (fn-traced [{:keys [db limited-uses active-sheet-id]} [trigger]]
    {:db (update-in db [:sheets active-sheet-id :limited-uses]
                    apply-limited-use-trigger
                      (reduce
                      (fn [m v]
                        (assoc m (:id v) v))
                      {}
                      limited-uses)
                    trigger)}))
