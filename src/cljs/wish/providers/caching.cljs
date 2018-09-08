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
    (provider/init! base))

  (load-raw
    [this id]
    (go (let [[err resp :as result] (<! (provider/load-raw
                                          base id))]
          (if-not err
            (do
              (log "write to cache: " id)
              (swap! storage assoc id resp)
              result)

            (or (when-let [data (get @storage id)]
                  [nil data])
                result)))))

  (query-data-sources [this]
    (provider/query-data-sources base))

  (register-data-source [this]
    (provider/register-data-source base))

  (save-sheet [this file-id data]
    (log/todo "Save-sheet to cache: " file-id)
    (provider/save-sheet base file-id data)))

(defn with-caching [base-provider]
  (let [cache-id (keyword (str (name (provider/id base-provider))
                               "-cached"))]
    (->CachingProvider base-provider
                       cache-id
                       (local-storage (atom nil) cache-id))))
