(ns ^{:author "Daniel Leong"
      :doc "util.worker"}
  wish.util.worker
  (:require-macros [wish.util.log :as log :refer [log]])
  (:require [cljs.reader :as edn]
            [goog.events :as gevents :refer [EventType]]
            [wish.config :as config]))

(defonce ^:private message-listener-key (atom nil))

; ======= internal functions ==============================

(defn receive! [[msg-type & args :as msg]]
  (case msg-type
    :shell-updated (let [[modified] args]
                     (log/todo "shell-updated: " modified))
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
                           (.-MESSAGE EventType)
                           (fn [evt]
                             (-> evt
                                 (.getBrowserEvent)
                                 (.-data)
                                 edn/read-string
                                 receive!)))]

             ; let the serviceWorker know we're listening
             (js/navigator.serviceWorker.controller.postMessage
               (str [:ready]))

             new-key))))

(defn register! []
  (-> js/navigator.serviceWorker
      (.register (str config/server-root "/worker.js"))
      (.then
        (fn [reg]
          (log "mounted service worker! " reg))
        (fn [e]
          (log/warn "error mounting service worker: " e)))))

