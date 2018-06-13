(ns wish.events
  (:require [re-frame.core :refer [dispatch reg-event-db reg-event-fx
                                   trim-v]]
            [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
            [wish.db :as db]))

(reg-event-db
  ::initialize-db
  (fn-traced [_ _]
    db/default-db))

(reg-event-db
  :navigate!
  [trim-v]
  (fn-traced [db page-spec]
    (assoc db :page page-spec)))

(reg-event-db
  ::set-re-pressed-example
  (fn [db [_ value]]
    (assoc db :re-pressed-example value)))
