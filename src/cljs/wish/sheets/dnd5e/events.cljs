(ns ^{:author "Daniel Leong"
      :doc "dnd5e-specific events"}
  wish.sheets.dnd5e.events
  (:require [re-frame.core :refer [dispatch reg-event-db reg-event-fx
                                   trim-v]]
            [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
            [wish.sheets.util :refer [update-uses]]))

(reg-event-db
  ::update-hp
  [trim-v]
  (fn-traced [db _]
    (update-uses db :hp#uses inc)))

