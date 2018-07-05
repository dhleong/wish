(ns ^{:author "Daniel Leong"
      :doc "Builtin provider for data sources"}
  wish.providers.wish
  (:require-macros [wish.util.log :refer [log]])
  (:require [clojure.core.async :refer [chan put! to-chan <! >!]]
            [ajax.core :refer [GET]]
            [wish.config :as config]
            [wish.providers.core :refer [IProvider]]
            [wish.sheets.util :refer [make-id]]
            [wish.util :refer [>evt]]))

(def ^:private data-root (str config/server-root
                              "/sources"))

(def ^:private builtin-sources
  {"dnd5e-srd" {:name "D&D 5e System Reference Document"
                :path "/dnd5e.edn"}})

(deftype WishProvider []
  IProvider
  (id [this] :wish)
  (create-sheet [this file-name data]
    (to-chan [[(js/Error. "Not implemented") nil]]))

  (init! [this]) ; nop

  (load-raw
    [this id]
    (let [ch (chan)
          url (str data-root (:path (builtin-sources id)))]
      (if url
        (GET url
             {:handler (fn [raw]
                         (log "Loaded " url)
                         (put! ch [nil raw]))
              :error-handler (fn [e]
                               (put! ch [e]))})

        (put! ch [(js/Error. (str "No such source: " id))]))

      ; return the ch
      ch))

  (query-data-sources [this]
    (>evt [:add-data-sources
           (map (fn [[str-id info]]
                  (assoc info :id (make-id :wish str-id)))
                builtin-sources)]))

  (register-data-source [this]
    (to-chan [[(js/Error. "Not implemented") nil]]))

  (save-sheet [this file-id data]
    (to-chan [[(js/Error. "Not implemented") nil]])))

(defn create-provider []
  (->WishProvider))
