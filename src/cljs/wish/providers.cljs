(ns ^{:author "Daniel Leong"
      :doc "Data source providers"}
  wish.providers
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [wish.util.log :as log :refer [log]])
  (:require [clojure.core.async :as async :refer [<!]]
            [clojure.string :as str]
            [cljs.reader :as edn]
            [wish.providers.caching :refer [with-caching]]
            [wish.providers.gdrive :as gdrive]
            [wish.providers.gdrive.config :as gdrive-config]
            [wish.providers.gdrive.errors :as gdrive-errors]
            [wish.providers.wish :as wish]
            [wish.providers.core :as provider]
            [wish.sheets.util :refer [unpack-id]]
            [wish.util :refer [>evt]]
            [wish.views.widgets :as widgets]))

(def ^:private providers
  {:gdrive
   {:id :gdrive
    :name "Google Drive"
    :config #'gdrive-config/view
    :error-resolver #'gdrive-errors/view
    :share! #'gdrive/share!
    :inst (with-caching
            (gdrive/create-provider))}

   :wish
   {:id :wish
    :name "Wish Built-ins"
    :inst (wish/create-provider)}})

(defonce ^:private last-data-source-query (atom 0))

; at least 30 seconds
(def ^:private min-ms-between-queries (* 30 1000))

(defn get-info
  [provider-id]
  (get providers provider-id))

(defn- provider-key
  "Convenience for user with if-let"
  [provider-id k]
  (->> provider-id
       get-info
       k))

(defn config-view
  [provider-id]
  (if-let [config (provider-key provider-id :config)]
    [config]

    [:div.error "No config for this provider"]))

(defn error-resolver-view
  [{:keys [provider id] :as data}]
  (if-let [error-resolver (provider-key provider :error-resolver)]
    [error-resolver data]

    [widgets/error-box data]))

(defn init! []
  (log/info "init!")

  ; prep provider states all at once, to prevent :wish
  ; from drowning out others
  (>evt [:prepare-provider-states! (keys providers)])

  ; let every provider init! in parallel, waiting for each to
  ; let us know what their state is before putting it into the DB
  (go-loop [init-chs (->> providers
                          vals
                          (map (fn [provider]
                                 (go {:provider-id (:id provider)
                                      :state (<! (provider/init!
                                                   (:inst provider)))}))))]
    (let [[{:keys [provider-id state]} port] (alts! init-chs)
          new-chs (filterv
                    (partial not= port)
                    init-chs)]
      (>evt [:put-provider-state! provider-id state])
      (if (empty? new-chs)
        (log/info "init!'d all providers")

        ; still waiting
        (recur new-chs)))))

(defn sharable? [sheet-id]
  (let [[provider-id _] (unpack-id sheet-id)]
    (provider-key provider-id :share!)))

(defn create-sheet-with-data
  "Returns a channel that emits [err sheet-id] on success"
  [sheet-name provider-id data]
  {:pre [(not (nil? provider-id))
         (not (nil? data))]}
  (if-let [inst (provider-key provider-id :inst)]
    (provider/create-sheet inst
                           sheet-name
                           data)

    (throw (js/Error. (str "No provider instance for " provider-id)))))

(defn load-raw
  "Load raw data for the given ID, formatted the same as a sheet-id
   (IE: the provider id is the namespace, and the data id is the name.)"
  [raw-id]
  (let [[provider-id pro-raw-id] (unpack-id raw-id)]
    (if-let [inst (provider-key provider-id :inst)]
      (do
        (log "Load " pro-raw-id " from " provider-id)
        (provider/load-raw inst pro-raw-id))

      (throw (js/Error. (str "No provider instance for " raw-id
                             "(" provider-id " / " pro-raw-id ")"))))))

(defn load-sheet!
  [sheet-id]
  (log "Load sheet " sheet-id)
  (go (let [[err data] (<! (load-raw sheet-id))
            [sheet-err sheet] (when data
                                (try
                                  [nil (edn/read-string data)]
                                  (catch :default e
                                    [e nil])))]
        (if-let [e (or err sheet-err)]
          (do (log/err "Failed to load sheet: " e)
              (>evt [:put-sheet-error! sheet-id
                     {:err e
                      :retry-evt [:load-sheet! sheet-id]}]))

          (>evt [:put-sheet! sheet-id sheet])))))

(defn register-data-source
  [provider-id]
  (if-let [inst (provider-key provider-id :inst)]
    (go (let [[err source] (<! (provider/register-data-source inst))]
          (cond
            err (log/warn "Error registering source" err)
            source (do
                     (>evt [:add-data-sources [source]])
                     (log "Registered " source))
            :else (log "Canceled registering source"))))

    (throw (js/Error. (str "No such provider " provider-id)))))

(defn query-data-sources!
  "Triggers an async query of available datasources from configured providers"
  []
  (let [last-query @last-data-source-query
        now (js/Date.now)]
    (when (> (- now last-query)
             min-ms-between-queries)
      (log "Initiate data source query")
      (reset! last-data-source-query now)
      (doseq [{:keys [inst]} (vals providers)]
        (when inst
          (provider/query-data-sources inst))))))

(defn query-sheets
  "Start querying the given provider-id for its sheets"
  [provider-id]
  (if-let [inst (provider-key provider-id :inst)]
    (go (let [[err sheets] (<! (provider/query-sheets inst))]
          (if err
            (log/warn "Failed to query " provider-id ": " err)
            (>evt [:add-sheets sheets]))

          ; either way, we're done querying:
          (>evt [:mark-provider-listing! provider-id false])))

    (log/err "No such provider to query: " provider-id)))

; format sheet data for saving (as a string). this should convert
; to a format for suitable for use with `load-raw`. For now, (str)
; does the trick
(def format-sheet-data str)

(defn save-sheet!
  [sheet-id data on-done]
  (let [[provider-id pro-sheet-id] (unpack-id sheet-id)]
    (if-let [inst (provider-key provider-id :inst)]
      (go (let [[err] (<! (provider/save-sheet
                            inst pro-sheet-id
                            data (format-sheet-data data)))]
            (on-done err)))

      (on-done (js/Error. (str "No provider instance for " sheet-id
                               "(" provider-id " / " pro-sheet-id ")"))))))

(defn share!
  [sheet-id]
  ; TODO should this be a provider method?
  (let [[provider-id pro-sheet-id] (unpack-id sheet-id)]
    (if-let [share! (provider-key provider-id :share!)]
      (share! pro-sheet-id)

      (throw (js/Error. (str "No provider instance for " sheet-id
                               "(" provider-id " / " pro-sheet-id ")"))))))
