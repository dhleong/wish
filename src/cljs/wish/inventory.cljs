(ns ^{:author "Daniel Leong"
      :doc "Inventory-related util functions"}
  wish.inventory
  (:require [clojure.string :as str]))

(def stackable-types #{:ammunition
                       :potion})

(def custom-namespace "custom")

(defn custom?
  "Given an item map, check if it's a custom item"
  [item]
  (or (nil? (:id item))
      (= custom-namespace
         (namespace (:id item)))))

(defn custom-id
  "Given the name of an item, generate a custom item-id for it"
  [item-name]
  (keyword custom-namespace
           (str
             "w"
             (str/replace
               item-name
               #"[^a-zA-Z0-9]" "")
             "-"
             (js/Date.now))))

(defn instanced?
  "Check if a given item is instanced? (IE: has its own entry in
   :meta/items)"
  [item]
  (or (not= (:id item)
            (:item-id item))
      (= custom-namespace (namespace (:id item)))))

(defn instantiate-id
  "Given an item ID, return a NEW instance id for it.
   Note that custom items typically just use their item-id
   for the instance-id; this method doesn't handle that"
  [item-id]
  (keyword (namespace item-id)
           (str (name item-id)
                "-inst-"
                (js/Date.now))))

(defn stacks?
  "Returns true if the given item should stack"
  [item]
  (or (:stacks? item)
      (when (stackable-types (:type item))
        true)
      false))
