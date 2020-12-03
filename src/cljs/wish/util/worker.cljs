(ns ^{:author "Daniel Leong"
      :doc "util.worker"}
  wish.util.worker
  (:require-macros [wish.util.log :as log :refer [log]])
  (:require [cljs.reader :as edn]
            [goog.events :as gevents]
            [wish.config :as config]
            [wish.util :refer [>evt]]))

(defonce ^:private message-listener-key (atom nil))

; ======= internal functions ==============================

(defn receive! [[msg-type & args :as msg]]
  (case msg-type
    :shell-updated (let [[modified] args]
                     (log/info "shell-updated: " modified)
                     (>evt [:set-latest-update modified]))
    (log "receive!'d unknown message from worker: " msg)))


; ======= external entry points ===========================

(defn listen! []
  (swap! message-listener-key
         (fn [old-key]
           ; stop listening to the old key
           (when old-key
             (log "removing old message listener")
             (gevents/unlistenByKey old-key))

           (let [new-key (gevents/listen
                           js/navigator.serviceWorker
                           gevents/EventType.MESSAGE
                           (fn [^js evt]
                             (-> evt
                                 (.getBrowserEvent)
                                 (.-data)
                                 edn/read-string
                                 receive!)))]

             (>evt [:set-worker-ready])

             new-key))))

(defn register! []
  (-> js/navigator.serviceWorker
      (.register (str config/server-root "/worker.js"))
      (.then
        (fn [reg]
          (log "mounted service worker! " reg))
        (fn [e]
          (log/warn "error mounting service worker: " e)))))

