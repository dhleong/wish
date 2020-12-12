(ns ^{:author "Daniel Leong"
      :doc "Builtin provider for data sources"}
  wish.providers.wish
  (:require [clojure.core.async :refer [to-chan!]]
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
  (id [_] :wish)
  (create-file [_ _kind _file-name _data]
    (to-chan! [[(js/Error. "Not implemented") nil]]))

  (init! [_]
    ; we're always ready immediately
    (to-chan! [:ready]))

  (connect! [_]
    ; not supported
    nil)

  (disconnect! [_]
    ; not supported
    nil)

  (load-raw
    [_ id]
    (if-let [url (str data-root (:path (builtin-sources id)))]
      (GET url {:response-format :text})

      (to-chan! [[(js/Error. (str "No such source: " id))]])))

  (query-data-sources [_]
    (>evt [:add-data-sources
           (map (fn [[str-id info]]
                  (assoc info :id (make-id :wish str-id)))
                builtin-sources)]))

  (query-sheets [_]
    ; we never provide any sheets
    (to-chan! [[nil nil]]))

  (register-data-source [_]
    (to-chan! [[(js/Error. "Not implemented") nil]]))

  (save-sheet [_ _file-id _data _data-str]
    (to-chan! [[(js/Error. "Not implemented") nil]]))

  (watch-auth [_]
    ; not supported
    nil))

(defn create-provider []
  (->WishProvider))
