(ns ^{:author "Daniel Leong"
      :doc "caching"}
  wish.providers.caching
  (:require-macros [cljs.core.async :refer [go]]
                   [wish.util.log :as log :refer [log]])
  (:require [clojure.core.async :refer [alt! chan close! timeout <! >!]]
            [alandipert.storage-atom :refer [local-storage]]
            [wish.providers.core :as provider :refer [IProvider signed-out-err?]]
            [wish.sheets.util :refer [make-id]]
            [wish.util :refer [>evt]]))

(def default-timeout 7500)
(def save-sheet-timeout 25000)

(defn- undirty
  "Persist the set of dirty files with the given ids"
  [base storage dirty?-storage ids]
  (log "Persist dirty files" ids)
  (doseq [id ids]
    (if-let [data-str (get @storage id)]
      (>evt [:persist-cached-sheet!
             (make-id (provider/id base) id)
             data-str])

      (do (log/warn "Dirty flag set for " id " but no data stored!")
          (swap! dirty?-storage disj id)))))

(defn <!timeout
  "Take from the channel as <!, but give up after a timeout. In some slow
   networks (like when on the subway) it may seem that we're fetching, but
   (especially in cases like gapi init) it may effectively never complete,
   and our service worker may not be able to do anything about it (for example,
   service worker can timeout the core gapi lib, but has no jurisdiction over
   the libs it subsequently tries to load).

   By timing out at the cache layer, we can share this functionality across
   things that we want cached and let the Providers not have to worry about
   timeouts.

   Returns `nil` on timeout"
  ([ch]
   (<!timeout ch default-timeout))
  ([ch timeout-ms]
   (go (let [to (timeout timeout-ms)]
         (alt!
           ch ([v] v)
           to nil)))))

(deftype CachingProvider [base my-id storage dirty?-storage]
  IProvider
  (id [this] my-id)
  (create-file [this kind file-name data]
    (provider/create-file base kind file-name data))

  (init! [this]
    (go (let [base-state (or (<! (<!timeout (provider/init! base)))
                             :unavailable)]
          (when (= :ready base-state)
            (when-let [ids (seq @dirty?-storage)]
              (undirty base storage dirty?-storage ids)))

          (if-not (= :unavailable base-state)
            base-state

            ; if base is unavailable, we can take over
            :cache-only))))

  (disconnect! [this]
    ; clear cache
    (reset! storage nil)

    ; delegate to base
    (provider/disconnect! base))

  (load-raw
    [this id]
    (go (let [is-dirty? (contains? @dirty?-storage id)
              [err resp :as result] (when-not is-dirty?
                                      ; don't try to load from provider if dirty
                                      (or (<! (<!timeout (provider/load-raw
                                                            base id)))
                                          [:timeout]))]
          (or
            ; if the sheet cache is dirty, just load it from cache to avoid
            ; overwriting changes accidentally
            (when is-dirty?
              (when-let [data (get @storage id)]
                (log/info "loaded dirty " id " from cache")
                [nil data]))

            ; successful load
            (when-not err
              (log/info "write to cache: " id)
              (swap! storage assoc id resp)
              result)

            ; couldn't load because we signed out of the provider;
            ; remove anything we had from the cache and return
            ; the original result
            (when (signed-out-err? err)
              (log/info "remove " id " from cache")
              (swap! storage dissoc id)
              result)

            ; some other error loading; try the cache
            (when-let [data (get @storage id)]
              (log/info "loaded " id " from cache")
              [nil data])

            ; no cache backup; just return the result
            result))))

  (query-data-sources [this]
    (provider/query-data-sources base))

  (query-sheets [this]
    (let [ch (chan)
          from-cache (get @storage ::sheets)]
      (go
        (when from-cache
          (log/info "loaded sheets from cache")
          (>! ch [nil from-cache]))

        (let [[err sheets :as result] (or (<! (<!timeout (provider/query-sheets base)))
                                          [:timeout])]
          (log "query-sheets: " err (count sheets))
          (if-not err
            (do
              ; cache sheets
              (swap! storage assoc ::sheets sheets)
              (>! ch result))

            ; fetch from cache
            (when-not from-cache
              (log/info "no sheets in cache")))

          ; either way, close the channel
          (close! ch)))
      ch))

  (register-data-source [this]
    (provider/register-data-source base))

  (save-sheet [this file-id data data-str]
    (log/info "save-sheet to cache: " file-id)
    (swap! storage assoc file-id data-str)
    (swap! dirty?-storage conj file-id)
    (go (let [[err _ :as result] (or (<! (<!timeout (provider/save-sheet
                                                       base file-id data data-str)
                                                     save-sheet-timeout))
                                     [:timeout])]
          (when-not err
            ; remove dirty flag
            (swap! dirty?-storage disj file-id))

          ; return the result as-is
          result)))

  (watch-auth [this]
    ; delegate
    (provider/watch-auth base)))

(defn with-caching [base-provider]
  (let [cache-id (keyword (str (name (provider/id base-provider))
                               "-cached"))
        dirty?-id (keyword (str (name (provider/id base-provider))
                               "-dirty?"))]
    (->CachingProvider base-provider
                       cache-id
                       (local-storage (atom nil) cache-id)
                       (local-storage (atom #{}) dirty?-id))))
