(ns ^{:author "Daniel Leong"
      :doc "Data source providers"}
  wish.providers
  (:require [clojure.string :as str]
            [wish.providers.dummy :as dummy]
            [wish.sheets.util :refer [unpack-id]]))

(def ^:private providers
  {:dummy
   {:id :dummy
    :name "Dummy"
    :init! dummy/init!}})

(defn init! []
  (println "INIT!")
  (doseq [provider (vals providers)]
    (when-let [init-provider! (:init! provider)]
      (init-provider!))))

(defn load-sheet!
  [sheet-id]
  (let [[provider-id pro-sheet-id] (unpack-id sheet-id)]
    (when-let [{:keys [inst]} (get providers provider-id)]
      ;; TODO
      (println "LOAD " pro-sheet-id " FROM " inst))))
