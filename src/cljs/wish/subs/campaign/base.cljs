(ns wish.subs.campaign.base
  (:require [re-frame.core :refer [reg-sub]]))

; ======= util ============================================

(defn- reg-meta-sub
  [id getter]
  (reg-sub
    id
    :<- [:sheet-meta]
    (fn [sheet _]
      (getter sheet))))

(reg-meta-sub :meta/workspace :workspace)
(reg-meta-sub :meta/spaces :spaces)
