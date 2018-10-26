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
                ;; :path "/dnd5e.edn.json"
                :path "/dnd5e.transit.json"
                }})

(deftype WishProvider []
  IProvider
  (id [this] :wish)
  (create-sheet [this file-name data]
    (to-chan [[(js/Error. "Not implemented") nil]]))

  (init! [this]
    ; we're always ready immediately
    (to-chan [:ready]))

  (load-raw
    [this id]
    (let [ch (chan)
          url (str data-root (:path (builtin-sources id)))]
      (if url
        (GET url
             {:handler (fn [raw]
                         (log "Loaded " url)
                         (put! ch [nil raw]))
              :response-format :text
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

  (query-sheets [this]
    ; we never provide any sheets
    (to-chan [[nil nil]]))

  (register-data-source [this]
    (to-chan [[(js/Error. "Not implemented") nil]]))

  (save-sheet [this file-id data data-str]
    (to-chan [[(js/Error. "Not implemented") nil]]))

  (watch-auth [this]
    ; not supported:
    nil))

(defn create-provider []
  (->WishProvider))
