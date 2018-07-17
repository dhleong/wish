(ns ^{:author "Daniel Leong"
      :doc "Lists"}
  wish.sources.compiler.lists
  (:require-macros [wish.util.log :as log :refer [log]])
  (:require [clojure.string :as str]
            [wish.sources.core :as src]
            [wish.sources.compiler.entity :refer [compile-entity]]
            [wish.sources.compiler.feature :refer [compile-feature]]
            [wish.util :refer [->map]]))

(defn unpack-options-key
  "You can add the selected options from a feature to a list
   by appending >>options to the feature id keyword. This
   function unpacks such a keyword and returns the feature id,
   if it is such a key, or nil if not"
  [k]
  (let [n (name k)]
    (when (str/ends-with? n ">>options")
      (keyword (namespace k)
               (subs n 0 (- (count n)
                            (count ">>options")))))))

(defn- find-entity
  "Always returns a collection `id` could point to a feature,
   a list, or an element in a list"
  [s id]
  (or (when-let [f (get-in s [:features id])]
        [f])
      (when-let [f (get-in s [:list-entities id])]
        [f])

      (get-in s [:lists id])

      ; if we have a data source (IE when inflating an entity),
      ; check it out
      (when-let [ds (:wish/data-source s)]
        (or
          (when-let [f (src/find-list-entity ds id)]
            [f])
          (when-let [f (src/find-feature ds id)]
            [f])))

      ; else:
      (log/warn "Unable to find entity " id)))

(declare inflate-items)

(defn- inflate-item
  "Always returns a collection"
  [s spec item]
  (try
    (cond
      (map? item) (if (= :feature (:type spec))
                    [(compile-feature item)]
                    [(compile-entity item)])
      (keyword? item) (if-let [options-feature-id (unpack-options-key item)]
                        ; inflate options from the feature
                        (->> s
                             :wish/options
                             options-feature-id
                             (inflate-items s))

                        ; just find
                        (find-entity s item))
      (coll? item) (mapcat (partial inflate-item s spec) item)
      :else (throw (js/Error. (str "Unexpected list item: " (type item) item ))))
    (catch :default e
      (throw (js/Error.
               (str "Error inflating " item " of " spec
                    "\n\nOriginal error: " (.-stack e)))))))

(defn inflate-items
  ([s items]
   (inflate-items s (meta items) items))
  ([s spec items]
   (with-meta
     (mapcat
       (partial inflate-item s spec)
       items)
     (meta items))))

(defn add-to-list
  [s id-or-spec & items]
  (let [id (if (keyword? id-or-spec)
             id-or-spec
             (:id id-or-spec))
        spec (when (map? id-or-spec)
               (dissoc id-or-spec :id))
        inflated-items (inflate-items s spec items)
        dest-key (if (= :feature (:type spec))
                   :features
                   :list-entities)
        inflated-map (->> inflated-items
                          (filter :id)
                          ->map)]
    (-> s
        (update dest-key merge inflated-map)

        (update-in [:lists id]
                   (fn [existing]
                     (let [m (or spec
                                 (meta existing))]
                       (with-meta
                         (concat existing
                                 inflated-items)
                         m)))))))
