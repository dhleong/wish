(ns ^{:author "Daniel Leong"
      :doc "Lists"}
  wish.sources.compiler.lists
  (:require [wish.util :refer [->map]]))

(defn- find-entity
  "Always returns a collection `id` could point to a feature,
   a list, or an element in a list"
  [s id]
  (or (when-let [f (get-in s [:features id])]
        [f])
      (when-let [f (get-in s [:list-entities id])]
        [f])
      (get-in s [:lists id])))

(defn- inflate-item
  "Always returns a collection"
  [s item]
  (cond
    (map? item) [item]
    (keyword? item) (find-entity s item)
    (coll? item) (mapcat (partial inflate-item s) item)
    :else (throw (js/Error. (str "Unexpected list item: " item)))))

(defn inflate-items
  [s items]
  (with-meta
    (mapcat
      (partial inflate-item s)
      items)
    (meta items)))

(defn add-to-list
  [s id-or-spec & items]
  (let [id (if (keyword? id-or-spec)
             id-or-spec
             (:id id-or-spec))
        spec (when (map? id-or-spec)
               (dissoc id-or-spec :id))
        inflated-items (inflate-items s items)]
    (-> s
        (update :list-entities merge (->> inflated-items
                                          (filter :id)
                                          ->map))
        (update-in [:lists id]
                   (fn [existing]
                     (let [m (or spec
                                 (meta existing))]
                       (with-meta
                         (concat existing
                                 inflated-items)
                         m)))))))
