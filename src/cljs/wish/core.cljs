(ns wish.core
  (:require-macros [wish.util.log :as log :refer [log]])
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [re-pressed.core :as rp]
            [wish.events :as events]
            [wish.routes :as routes]
            [wish.views :as views]
            [wish.config :as config]
            [wish.fx]))


(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-worker []
  (when js/navigator.serviceWorker
    (log "mount worker")
    (-> js/navigator.serviceWorker
        (.register (str config/server-root "/worker.js"))
        (.then
          (fn [reg]
            (log "mounted service worker! " reg))
          (fn [e]
            (log/warn "error mounting service worker: " e))))))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (when config/debug?
    ; hot-reload providers
    (wish.providers/init!))
  (reagent/render [views/main]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (routes/app-routes)
  (re-frame/dispatch-sync [::events/initialize-db])
  (re-frame/dispatch-sync [::rp/add-keyboard-event-listener "keydown"])
  (dev-setup)
  (mount-worker)
  (mount-root))
