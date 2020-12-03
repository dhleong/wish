(ns ^{:author "Daniel Leong"
      :doc "It watches the network"}
  wish.util.netwatcher
  (:require-macros [wish.util.log :refer [log]])
  (:require [goog.events :as events]
            [wish.util :refer [>evt]]))

(defonce ^:private network-listener-key (atom nil))

(defn on-network-changed [e]
  (let [new-state (-> e .-type keyword)]
    (log "network state <- " new-state)
    (>evt [:set-online (= :online new-state)])))

(defn attach! []
  ; we use this pattern mostly for dev purposes.
  ; in production, attach! should only be called once
  (swap! network-listener-key
         (fn [old-key]
           ; stop listening to the old key
           (when old-key
             (log "removing old netwatcher")
             (events/unlistenByKey old-key))

           (events/listen
             js/window
             #js [events/EventType.OFFLINE
                  events/EventType.ONLINE]
             on-network-changed))))
