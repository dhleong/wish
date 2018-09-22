(ns ^{:author "Daniel Leong"
      :doc "media-tracker"}
  wish.views.widgets.media-tracker
  (:require-macros [wish.util.log :refer [log]])
  (:require [reagent.core :as r]
            [wish.util :refer [>evt]]))

(defn media-tracker
  "Creates a dummy node in the DOM that tracks the media type and
   dispatches events based on the ones that matched. `clauses` consists
   of pairs of media queries and event vectors, somewhat like a `(case)`,
   with an optional default event vector last. The first media query
   that matches is the one that will be dispatched."
  [& clauses]
  (when (seq clauses)
    (when-let [create-matcher js/window.matchMedia]
      (log "Attach media trackers")
      (r/with-let [pairs (partition-all 2 clauses)
                   has-default? (= 1 (count (last pairs)))
                   default (when has-default?
                             (first (last pairs)))
                   pairs (if has-default?
                           (drop-last pairs)
                           pairs)

                   queries (->> pairs
                                (map (fn [[query-str event]]
                                       [(create-matcher (str query-str)) event]))
                                (into {}))

                   handler #(loop [candidates queries]
                              (let [[candidate event] (first candidates)]
                                (cond
                                  (nil? candidate)
                                  (when default
                                    (>evt default))

                                  (.-matches candidate)
                                  (do
                                    (>evt event))

                                  :else
                                  (do
                                    (recur (next candidates))))))

                   _ (doseq [matcher (keys queries)]
                       (.addListener matcher handler))

                   ; try immediately
                   _ (handler)]

        ; don't render anything
        nil

        (finally
          (log "Detach media tracker")
          (doseq [matcher (keys queries)]
            (.removeListener matcher handler)))))))
