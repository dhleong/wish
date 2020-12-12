(ns wish.providers.demo
  (:require [clojure.core.async :refer [to-chan!]]
            [clojure.pprint :refer [pprint]]
            [alandipert.storage-atom :refer [local-storage]]
            [wish.config :as config]
            [wish.providers.core :refer [IProvider]]
            [wish.sheets.util :as sheets]
            [wish.util :refer [>evt]]
            [wish.util.http :refer [GET]]))

(def ^:private data-root (str config/server-root
                              "/demo"))

(def ^:private demo-sheets
  {"mreynolds" {:name "Mal Reynolds"}
   "katara" {:name "Katara"}})

(defn- list-sheets []
  (for [[id data] demo-sheets]
    [(sheets/make-id :demo id)
     (merge { :type :sheet
             :mine? true}
            (select-keys data [:name]))]))

(deftype DemoProvider [provider-state]
  IProvider
  (id [_] :demo)
  (create-file [_ _kind _file-name _data]
    (to-chan! [[(js/Error. "Not implemented") nil]]))

  (init! [_]
    ; TODO configure state
    (if (true? (:enabled @provider-state))
      (to-chan! [:ready])
      (to-chan! [:signed-out])))

  (connect! [_]
    (swap! provider-state assoc :enabled true)
    (>evt [:put-provider-state! :demo :ready]))

  (disconnect! [_]
    (swap! provider-state dissoc :enabled)
    (>evt [:put-provider-state! :demo :signed-out]))

  (load-raw [_ id]
    (if (demo-sheets id)
      (GET (str data-root "/" id ".json")
           {:response-format :text})

      (to-chan! [[(js/Error. (str "No such sheet: " id))]])))

  (query-data-sources [_]
    ; nop
    )

  (query-sheets [_]
    (to-chan! [[nil (list-sheets)]]))

  (register-data-source [_]
    (to-chan! [[(js/Error. "Not implemented") nil]]))

  (save-sheet [_ file-id data data-str]
    (println "'Saving' " file-id ": ")
    (if config/debug?
      (pprint data)
      (println data-str))
    (to-chan! [[nil nil]]))

  (watch-auth [_]
    ; not supported
    nil))

(defn create-provider []
  (->DemoProvider
    (local-storage (atom nil) "demo-provider-state")))
