(ns ^{:author "Daniel Leong"
      :doc "fx"}
  wish.fx
  (:require-macros [wish.util.log :as log :refer [log]])
  (:require [re-frame.core :refer [reg-fx]]
            [re-pressed.core :as rp]
            [wish.db :as db]
            [wish.sources :as sources]
            [wish.providers :as providers :refer [load-sheet! save-sheet!]]
            [wish.sheets :as sheets]
            [wish.util :refer [>evt]]))

; ======= html stuff =======================================

(reg-fx
  :title!
  (fn [title]
    (log "document.title <-" title)
    (set! js/document.title title)))


; ======= keymaps =========================================

(reg-fx
  ::update-keymaps
  (fn [[[page-id :as page] sheet-kind]]
    (let [new-keymaps
          (case page-id
            :sheet (sheets/get-keymaps sheet-kind)

            ; otherwise, just clear
            nil)]
      (>evt [::rp/set-keydown-rules new-keymaps]))))


; ======= provider-related =================================

(reg-fx :providers/init! providers/init!)
(reg-fx :providers/query-data-sources providers/query-data-sources!)
(reg-fx :providers/query-sheets (fn [provider-id]
                                  (when provider-id
                                    (providers/query-sheets provider-id))))


; ======= sheet load requests ==============================

(reg-fx :load-sheet! load-sheet!)
(reg-fx :load-sheet-source!
        (fn [[sheet sources]]
          (sources/load! sheet sources)))


; ======= Sheet persistence ================================

(def ^:private log-sheet (log/make-fn "save-sheet"))

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
  (fn [[sheet-id data data-preformatted?]]
    (>evt [::db/mark-save-processing sheet-id])

    (save-sheet!
      sheet-id data data-preformatted?
      (fn on-saved [err]
        (log-sheet "on-saved(" sheet-id ") " err)

        (when err
          (log/err "Error saving " sheet-id ": " err))

        (when-not err
          (js/window.removeEventListener
            "beforeunload"
            confirm-close-window))

        (>evt [::db/finish-save sheet-id err])))))

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
      (log-sheet "scheduled:" sheet-id)
      (swap! save-sheet-timers
             assoc
             sheet-id
             (js/setTimeout
               (fn []
                 (log-sheet "performing:" sheet-id)
                 (swap! save-sheet-timers dissoc sheet-id)
                 (>evt [::save-sheet! sheet-id]))
               throttled-save-timeout)))))

; ======= Sheet misc ======================================

(reg-fx
  :share-sheet!
  (fn [sheet-id]
    (providers/share! sheet-id)))


; ======= App updates =====================================

(reg-fx
  :update-app
  (fn []
    (js/location.reload)))
