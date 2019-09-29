(ns wish.subs.campaign.workspace
  (:require [re-frame.core :refer [reg-sub]]
            [wish.subs.campaign.base]
            #_[wish.subs.campaign.notes :as notes]
            #_[wish.util :refer [->map]]))

(defn inflate-entity
  [notes entity-id]
  ; TODO support more than just notes
  (or (when-let [n (notes entity-id)]
        (assoc n :kind :note))))

(reg-sub
  ::spaces-by-id
  :<- [:meta/spaces]
  (fn [#_spaces]
    #_(->map spaces)
    {:s/serenity
     {:id :s/serenity
      :p :n/serenity}}))

(reg-sub
  ::fake-notes-by-id
  (fn []
    {:n/serenity
     {:id :n/serenity
      :n "Serenity"
      :contents ["Text note"]
      }}))

(reg-sub
  ::fake-workspace
  (fn []
    [:s/serenity]))


(reg-sub
  ::spaces
  :<- [::spaces-by-id]

  ;; :<- [::notes/by-id]
  :<- [::fake-notes-by-id] ; TODO restore above

  ;; :<- [:meta/workspace]
  :<- [::fake-workspace] ; TODO restore above
  (fn [[spaces notes workspace]]
    ; TODO
    (let [inflate (partial inflate-entity notes)]
      (->> workspace
           (map spaces)
           (map (fn [s]
                  (-> s
                      (assoc :primary
                             (inflate (:p s)))
                      (assoc :secondary
                             (map inflate (:s s))))))))))
