(ns ^{:author "Daniel Leong"
      :doc "Data source providers"}
  wish.providers
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [clojure.core.async :refer [<!]]
            [clojure.string :as str]
            [wish.providers.dummy :as dummy]
            [wish.providers.gdrive :as gdrive]
            [wish.providers.gdrive.config :as gdrive-config]
            [wish.providers.core :as provider]
            [wish.sheets :refer [stub-sheet]]
            [wish.sheets.util :refer [unpack-id]]
            [wish.util :refer [>evt]]))

(def ^:private providers
  {:dummy
   {:id :dummy
    :name "Dummy"
    :inst (dummy/create-provider)}
   :gdrive
   {:id :gdrive
    :name "Google Drive"
    :config #'gdrive-config/view
    :inst (gdrive/create-provider)}})

(defn config-view
  [provider-id]
  (if-let [{:keys [config]} (get providers provider-id)]
    [config]

    [:div.error "No config for this provider"]))

(defn get-info
  [provider-id]
  (get providers provider-id))

(defn init! []
  (doseq [provider (vals providers)]
    (when-let [inst (:inst provider)]
      (provider/init! inst))))

(defn create-sheet!
  "Returns a channel that emits [err sheet-id] on success"
  [sheet-name provider-id sheet-kind]
  {:pre [(not (nil? provider-id))
         (not (nil? sheet-kind))]}
  (if-let [{:keys [inst]} (get providers provider-id)]
    (provider/create-sheet inst
                           sheet-name
                           (stub-sheet sheet-kind sheet-name))

    (throw (js/Error. (str "No provider instance for " provider-id)))))

(defn load-sheet!
  [sheet-id]
  (let [[provider-id pro-sheet-id] (unpack-id sheet-id)]
    (if-let [{:keys [inst]} (get providers provider-id)]
      (go (let [[err data] (<! (provider/load-sheet
                                 inst pro-sheet-id))]
            (if err
              ; TODO probably a :put-sheet-error! event, or something
              (println "Failed to load sheet: " err)

              (>evt [:put-sheet! sheet-id data]))))

      (throw (js/Error. (str "No provider instance for " sheet-id
                             "(" provider-id " / " pro-sheet-id ")"))))))

(defn save-sheet!
  [sheet-id data on-done]
  (let [[provider-id pro-sheet-id] (unpack-id sheet-id)]
    (if-let [{:keys [inst]} (get providers provider-id)]
      (go (let [[err] (<! (provider/save-sheet
                            inst pro-sheet-id data))]
            (on-done err)))

      (on-done (js/Error. (str "No provider instance for " sheet-id
                               "(" provider-id " / " pro-sheet-id ")"))))))
