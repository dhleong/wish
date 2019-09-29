(ns wish.subs.campaign.workspace
  (:require [re-frame.core :refer [reg-sub]]
            [wish.subs.campaign.base]
            [wish.util :refer [->map]]))

(reg-sub
  ::spaces-by-id
  :<- [:meta/spaces]
  (fn [spaces]
    (->map spaces)))

(reg-sub
  ::spaces
  :<- [::spaces-by-id]
  :<- [:meta/workspace]
  (fn [#_[spaces workspace]]
    ; TODO
    #_(->> workspace
         (map spaces)

         ; TODO inflate ids
         )

    [{:id :s/serenity
      :p {:id :n/serenity
          :n "Serenity"
          }}]
    ))
