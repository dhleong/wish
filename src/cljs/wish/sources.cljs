(ns ^{:author "Daniel Leong"
      :doc "Data Sources"}
  wish.sources
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [wish.util.log :as log])
  (:require [clojure.core.async :as async :refer [alts! <!]]
            [clojure.tools.reader.reader-types :refer [string-push-back-reader]]
            [cljs.reader :as edn]
            [wish.providers :as providers]
            [wish.sources.compiler :refer [compile-directives]]
            [wish.sources.core :as sources :refer [IDataSource ->DataSource id]]
            [wish.util :refer [>evt]]))

; cache of *compiled* sources by id
(defonce ^:private loaded-sources (atom {}))

(defn- compile-raw-source
  [id raw]
  (loop [reader (string-push-back-reader raw)
         directives []]
    (if-let [d (edn/read reader)]
      ; keep loading directives
      (recur reader (conj directives d))

      (->DataSource
        id
        (compile-directives
          directives)))))

(defn- load-source!
  "Returns a channel that signals with [err] or [nil source] when done"
  [source-id]
  (go (if-let [existing (get @loaded-sources source-id)]
        existing

        (let [[err raw] (<! (providers/load-raw source-id))]
          (if err
            [err]
            (let [compiled (compile-raw-source source-id raw)]
              ; cache the compiled source for for later
              (swap! loaded-sources assoc source-id compiled)

              [nil compiled]))))))

(defn- combine-sources!
  "Combine the given sources into a CompositeDataSource
   and save it to the app-db for the given sheet-id"
  [sheet-id sources]
  (>evt [:put-sheet-source!
         sheet-id
         (sources/composite sheet-id sources)]))

(defn load!
  [sheet-id sources]
  (let [existing @loaded-sources]
    (if (every? existing sources)
      (combine-sources! sheet-id
                        (map existing sources))

      (let [source-chs (map load-source! sources)
            total-count (count sources)]
        (log/info "load " sources)
        (go-loop [resolved []]
          (let [[[err loaded-src] _] (alts! source-chs)
                new-resolved (conj resolved loaded-src)]
            (when err
              ; TODO what do we do here?
              (log/err "ERROR loading a source " err))

            (if (= total-count (count new-resolved))
              ; DONE!
              (do
                (log/info "loaded" new-resolved)
                (combine-sources! sheet-id new-resolved))

              ; still waiting
              (do
                (log/info "loaded " (id loaded-src) "; still waiting...")
                (recur new-resolved)))))))))
