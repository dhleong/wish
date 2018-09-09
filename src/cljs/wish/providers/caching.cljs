(ns ^{:author "Daniel Leong"
      :doc "caching"}
  wish.providers.caching
  (:require-macros [cljs.core.async :refer [go]]
                   [wish.util.log :as log :refer [log]])
  (:require [clojure.core.async :refer [chan put! <!]]
            [alandipert.storage-atom :refer [local-storage]]
            [wish.providers.core :as provider :refer [IProvider]]))

(deftype CachingProvider [base my-id storage]
  IProvider
  (id [this] my-id)
  (create-sheet [this file-name data]
    (provider/create-sheet base file-name data))

  (init! [this]
    ; we have no init of our own; just delegate
    (provider/init! base))

  (load-raw
    [this id]
    (go (let [[err resp :as result] (<! (provider/load-raw
                                          base id))]
          (if-not err
            (do
              (log/info "write to cache: " id)
              (swap! storage assoc id resp)
              result)

            (or (when-let [data (get @storage id)]
                  (log/info "loaded " id " from cache")
                  [nil data])
                result)))))

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
    (provider/save-sheet base file-id data data-str)))

(defn with-caching [base-provider]
  (let [cache-id (keyword (str (name (provider/id base-provider))
                               "-cached"))]
    (->CachingProvider base-provider
                       cache-id
                       (local-storage (atom nil) cache-id))))
