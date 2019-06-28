(ns ^{:author "Daniel Leong"
      :doc "Builtin provider for data sources"}
  wish.providers.wish
  (:require [clojure.core.async :refer [chan put! to-chan <! >!]]
            [wish.config :as config]
            [wish.providers.core :refer [IProvider]]
            [wish.sheets.util :refer [make-id]]
            [wish.util :refer [>evt]]
            [wish.util.http :refer [GET]]))

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
  (create-file [this kind file-name data]
    (to-chan [[(js/Error. "Not implemented") nil]]))

  (init! [this]
    ; we're always ready immediately
    (to-chan [:ready]))

  (disconnect! [this]
    ; not supported
    nil)

  (load-raw
    [this id]
    (if-let [url (str data-root (:path (builtin-sources id)))]
      (GET url {:response-format :text})

      (to-chan [[(js/Error. (str "No such source: " id))]])))

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
    ; not supported
    nil))

(defn create-provider []
  (->WishProvider))
