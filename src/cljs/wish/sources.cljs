(ns ^{:author "Daniel Leong"
      :doc "Data Sources"}
  wish.sources
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [wish.util.log :as log :refer [log]])
  (:require [clojure.core.async :as async :refer [alts! <!]]
            [clojure.string :as str]
            [cljs.reader :as edn]
            [cognitect.transit :as t]
            [wish-engine.core :as engine]
            [wish-engine.edn :refer [edn-readers]]
            [wish-engine.state :as state]
            [wish.providers :as providers]
            [wish.sheets :as sheets]
            [wish.util :refer [>evt]]))

; cache of *compiled* sources by id
(defonce ^:private loaded-sources (atom {}))

(defn- read-transit-directives [raw]
  (let [raw (str/replace raw "\n" "\\n")]
    (t/read (t/reader :json) raw)))

(defn- read-edn-directives [raw]
  (edn/read-string {:readers edn-readers} (str "(do " raw ")"))
  #_(loop [reader (string-push-back-reader raw)
         directives []]
    (if-let [d #_(edn/read #_{:readers edn-readers} reader)]
      ; keep loading directives
      (recur reader (conj directives d))

      ; done!
      directives)))

(defn- compile-raw-source
  [{:keys [kind]} id raw]
  (let [directives (if (= (subs raw 0 2) "[[")
                     (do
                       (log "Read transit for " id)
                       (read-transit-directives raw))

                     (do
                       (log "Read edn for " id)
                       (read-edn-directives raw)))
        engine (sheets/get-engine kind)
        state (engine/create-state engine)]
    (log/time
      (str "Evaluate " (count directives) " directives for " id)
      (doseq [d directives]
        (engine/load-source engine state d)))
    state))

(defn- load-source!
  "Returns a channel that signals with [err] or [nil source] when done"
  [sheet source-id]
  (go (if-let [existing (get @loaded-sources source-id)]
        [nil {:id source-id :state existing}]

        (let [[err raw] (<! (providers/load-raw source-id))]
          (if err
            ; io error, or provider config error
            [err]

            (try
              (let [compiled (log/time
                               (str "compile:" source-id)
                               (compile-raw-source sheet source-id raw))]
                ; cache the compiled source for for later
                (swap! loaded-sources assoc source-id compiled)
                (log "Compiled " source-id compiled)

                [nil {:id source-id :state compiled}])

              (catch :default e
                ; error parsing raw source
                (log/err "Error parsing source:" source-id
                         "\n" (js/JSON.stringify raw))
                [e])))))))

(defn- combine-sources!
  "Combine the given sources and save it to the app-db for the given
   sheet-id"
  [sheet-id sources]
  (>evt [:put-sheet-source!
         sheet-id
         (apply state/composite sources)]))

(defn load!
  "Load the sources for the given sheet"
  [{sheet-id :id :as sheet} sources]
  (let [existing @loaded-sources]
    (if (every? existing sources)
      (combine-sources! sheet-id
                        (map existing sources))

      (let [source-chs (map (partial load-source! sheet) sources)
            total-count (count sources)]
        (log/info "load " sources)
        (go-loop [source-chs source-chs
                  resolved []]
          (let [[[err loaded-src-map] port] (alts! source-chs)
                {id :id loaded-src :state} loaded-src-map
                new-resolved (if err
                               resolved
                               (conj resolved loaded-src))]
            (log "loaded: " err loaded-src)
            (cond
              err
              (do
                (log/err "ERROR loading a source " err)
                (>evt [:put-sheet-error!
                       sheet-id
                       {:err err
                        :retry-evt [:load-sheet-source! sheet sources]}]))

              ; DONE!
              (= total-count (count new-resolved))
              (do
                (log/info "loaded" new-resolved)
                (combine-sources! sheet-id new-resolved))

              ; still waiting
              :else
              (do
                (log/info "loaded " (id loaded-src) "; still waiting...")
                (recur (filterv
                        (partial not= port)
                        source-chs)
                       new-resolved)))))))))

(defn unload! [source-id]
  (swap! loaded-sources dissoc source-id))
