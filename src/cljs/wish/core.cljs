(ns wish.core
  (:require-macros [wish.util.log :as log :refer [log]])
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [re-pressed.core :as rp]
            [wish.events :as events]
            [wish.routes :as routes]
            [wish.views :as views]
            [wish.config :as config]
            [wish.fx]
            [wish.subs]
            [wish.util.netwatcher :as netwatcher]
            [wish.util.worker :as worker]))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-worker []
  (when js/navigator.serviceWorker
    (log "mount worker")
    (worker/listen!)

    (worker/register!)))

(defn mount-root [& first?]
  (re-frame/clear-subscription-cache!)
  (when (and config/debug?
             (not first?))
    ; hot-reload providers; don't do it the first time,
    ; since it's requested as part of db init
    (wish.providers/init!))
  (netwatcher/attach!)
  (reagent/render [views/main]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [::events/initialize-db])
  (re-frame/dispatch-sync [::rp/add-keyboard-event-listener "keydown"])
  (routes/app-routes)
  (dev-setup)
  (mount-worker)
  (mount-root :first!))
