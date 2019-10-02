(ns wish.events.campaign.workspace
  (:require [re-frame.core :refer [reg-event-fx trim-v]]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [wish.sheets.util :refer [update-sheet-path]]))

(def ^:private drag-path-to-key
  {:secondary :s
   :primary :p})

(defn- vec-dissoc [v index]
  (vec
    (concat
      (subvec v 0 index)
      (subvec v (inc index)))))

(defn- vec-insert [v index value]
  (vec
    (concat
      (subvec v 0 index)
      [value]
      (subvec v index))))

(defn apply-drag [spaces {item :item
                          [from from-k from-idx] :from
                          [to to-k to-idx] :to}]
  (-> spaces
      (update-in [from (drag-path-to-key from-k)] vec-dissoc from-idx)
      (update-in [to (drag-path-to-key to-k)] vec-insert to-idx item)))

(reg-event-fx
  ::drag
  [trim-v]
  (fn-traced [cofx [ev]]
    (update-sheet-path cofx [:spaces]
                       (fn [spaces]
                         ; TODO
                         spaces
                         #_(apply-drag spaces ev)))))
