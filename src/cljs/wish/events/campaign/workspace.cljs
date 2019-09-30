(ns wish.events.campaign.workspace
  (:require [re-frame.core :refer [reg-event-fx trim-v]]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            ;; [vimsical.re-frame.cofx.inject :as inject]
            [wish.sheets.util :refer [update-sheet-path]]))

; TODO
;; (defn apply-drag [spaces {item :item
;;                           [from from-k from-idx] :from
;;                           [to to-k to-idx] :to}]
;;   )

(reg-event-fx
  ::drag
  [trim-v]
  (fn-traced [cofx [ev]]
    (update-sheet-path cofx [:spaces]
                       (fn [spaces]
                         ; TODO
                         spaces
                         #_(apply-drag spaces ev)))))
