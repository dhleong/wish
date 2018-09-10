(ns ^{:author "Daniel Leong"
      :doc "caching"}
  wish.providers.caching
  (:require-macros [cljs.core.async :refer [go go-loop]]
                   [wish.util.log :as log :refer [log]])
  (:require [clojure.core.async :refer [chan put! <!]]
            [alandipert.storage-atom :refer [local-storage]]
            [wish.providers.core :as provider :refer [IProvider]]
            [wish.sheets.util :refer [make-id]]
            [wish.util :refer [>evt]]))

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

(deftype CachingProvider [base my-id storage dirty?-storage]
  IProvider
  (id [this] my-id)
  (create-sheet [this file-name data]
    (provider/create-sheet base file-name data))

  (init! [this]
    (go (let [base-state (<! (provider/init! base))]
          (when (= :ready base-state)
            (when-let [ids (seq @dirty?-storage)]
              (undirty base storage dirty?-storage ids)))

          (if-not (= :unavailable base-state)
            base-state

            ; if base is unavailable, we can take over
            :cache-only))))

  (load-raw
    [this id]
    (go (let [is-dirty? (contains? @dirty?-storage id)
              [err resp :as result] (when-not is-dirty?
                                      ; don't try to load from provider if dirty
                                      (<! (provider/load-raw
                                            base id)))]
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

            ; error loading; try the cache
            (when-let [data (get @storage id)]
              (log/info "loaded " id " from cache")
              [nil data])

            ; no cache backup; just return the result
            result))))

  (query-data-sources [this]
    (provider/query-data-sources base))

  (query-sheets [this]
    (go (let [[err sheets :as result] (<! (provider/query-sheets base))]
          (log "query-sheets: " err (count sheets))
          (if-not err
            (do
              ; cache sheets
              (swap! storage assoc ::sheets sheets)
              result)

            ; fetch from cache
            (or (when-let [from-cache (get @storage ::sheets)]
                  (log/info "loaded sheets from cache")
                  [nil from-cache])

                (log/info "no sheets in cache")
                result)))))

  (register-data-source [this]
    (provider/register-data-source base))

  (save-sheet [this file-id data data-str]
    (log/info "save-sheet to cache: " file-id)
    (swap! storage assoc file-id data-str)
    (swap! dirty?-storage conj file-id)
    (go (let [[err _ :as result] (<! (provider/save-sheet
                                       base file-id data data-str))]
          (when-not err
            ; remove dirty flag
            (swap! dirty?-storage disj file-id))

          ; return the result as-is
          result))))

(defn with-caching [base-provider]
  (let [cache-id (keyword (str (name (provider/id base-provider))
                               "-cached"))
        dirty?-id (keyword (str (name (provider/id base-provider))
                               "-dirty?"))]
    (->CachingProvider base-provider
                       cache-id
                       (local-storage (atom nil) cache-id)
                       (local-storage (atom #{}) dirty?-id))))
