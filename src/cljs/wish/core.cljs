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
  ;; NOTE: right now, init! should only be done once, since it
  ;; sets each provider's state to `nil`. We could do that conditionally,
  ;; maybe, but actually none of our current providers do anything on
  ;; `init!`, so it's unclear if that's the correct behavior. Instead,
  ;; we simply do `init!` once for now, and leave this as reference in
  ;; case we add providers that need to re-init! somehow, and we can
  ;; figure out the best way to handle it then.
  ;; (when config/debug?
  ;;   ; hot-reload providers
  ;;   (wish.providers/init!))
  (reagent/render [views/main]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (routes/app-routes)
  (re-frame/dispatch-sync [::events/initialize-db])
  (re-frame/dispatch-sync [::rp/add-keyboard-event-listener "keydown"])
  (dev-setup)
  (mount-worker)
  (mount-root))
