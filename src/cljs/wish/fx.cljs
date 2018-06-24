(ns ^{:author "Daniel Leong"
      :doc "fx"}
  wish.fx
  (:require [re-frame.core :refer [reg-fx]]
            [wish.db :as db]
            [wish.sources :as sources]
            [wish.providers :as providers :refer [load-sheet! save-sheet!]]
            [wish.util :refer [>evt]]))

(reg-fx :providers/init! providers/init!)
(reg-fx :load-sheet! load-sheet!)
(reg-fx :load-sheet-source!
        (fn [[sheet-id sources]]
          (sources/load! sheet-id sources)))


; ======= Sheet persistence ================================

(defn confirm-close-window
  [e]
  (let [confirm-message "You have unsaved changes. Are you sure you want to exit?"]
    (when e
      (set! (.-returnValue e) confirm-message))
    confirm-message))

(defonce save-sheet-timers (atom {}))
(def throttled-save-timeout 7500)

(reg-fx
  ::save-sheet!
  (fn [[sheet-id data]]
    (>evt [::db/mark-save-processing sheet-id])

    (save-sheet!
      sheet-id data
      (fn on-saved [err]
        (println "on-saved " err)
        ; TODO indicate error; dispatch retry (later)
        (when-not err
          (js/window.removeEventListener
            "beforeunload"
            confirm-close-window)
          (js/console.log "Finished save of " sheet-id))

        (>evt [::db/finish-save sheet-id])))))

(reg-fx
  :schedule-save
  (fn [sheet-id]
    (when sheet-id
      ; NOTE: schedule-save can be conditional

      (if-let [timer (get @save-sheet-timers sheet-id)]
        ; existing timer; clear it
        (js/clearTimeout timer)

        ; no existing, so this is the first; confirm window closing
        (js/window.addEventListener
          "beforeunload"
          confirm-close-window))

      (>evt [::db/put-pending-save sheet-id])
      (js/console.log "Schedule save of" (str sheet-id))
      (swap! save-sheet-timers
             assoc
             sheet-id
             (js/setTimeout
               (fn []
                 (js/console.log "Perform save of" (str sheet-id))
                 (swap! save-sheet-timers dissoc sheet-id)
                 (>evt [::save-sheet! sheet-id]))
               throttled-save-timeout)))))

