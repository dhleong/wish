(ns ^{:author "Daniel Leong"
      :doc "Utilities for data-based entity modification"}
  wish.sources.compiler.entity-mod
  (:require [clojure.string :as str]))

(defn- mod-key
  ([k skip-chars]
   (let [n (name k)]
     (keyword (namespace k) (subs n skip-chars)))))

(declare apply-entity-mod)

(defn- newest-value [a b] b)

(defn idempotent-append
  [^String a ^String b]
  (if (str/ends-with? a b)
    a  ; no change
    (str a b)))

(defn- merge-features-list-into-map
  [features new-features]
  (reduce
    (fn [features-map new-feature]
      (let [new-feature-id (cond
                             ; unpack
                             (map-entry? new-feature)
                             (first new-feature)

                             ; keyword id
                             (keyword? new-feature)
                             new-feature

                             ; normal
                             :else
                             (:id new-feature))

            new-feature (if (map-entry? new-feature)
                          ; unpack
                          (second new-feature)

                          ; normal
                          new-feature)]
        (cond
          ; increment to support multiple feature instances.
          ; We ONLY support this when referenced by id
          (or (keyword? new-feature)
              (not (:id new-feature)))
          (-> features-map
              (update-in [new-feature-id :wish/instances] inc)

              ; each instance of a feature provided by :+features should
              ; have its own :wish/sort, provided by compile-entity. Since
              ; instanced features are managed by a single map for the original
              ; feature, we conj all the :wish/sort entries together and pull
              ; them out in the subscription when the feature is being expanded
              ; into its instances
              (cond-> (:wish/sort new-feature)
                (update-in [new-feature-id :wish/sorts]
                           conj (:wish/sort new-feature))))

          (map? new-feature)
          (assoc features-map new-feature-id new-feature)
          )))
    features
    new-features))

; NOTE public for testing
(defn extract-mod-and-key
  "Given an entity-mod keyword, pick the function used
   to merge the keys and figure out the original keyword"
  [k]
  (let [n (name k)]
    (case (first n)
      \+ (let [the-key (mod-key k 1)]
           (if (= the-key :features)
             ; :features is a special case because the edn list
             ; actually gets inflated into a map
             [merge-features-list-into-map the-key]

             ; normal case
             [concat the-key]))

      \> (if (not= \> (second n))
           (throw (js/Error. (str "Invalid mod prefix on key " k)))
           [idempotent-append (mod-key k 2)])

      \& [apply-entity-mod (mod-key k 1)]

      \= [newest-value (mod-key k 1)]

      ; otherwise, just replace:
      [newest-value k])))

(defn apply-entity-mod
  "Apply a mod-map to the given entity. This is basically a
   special version of (merge) that supports special key prefixes
   to performing a specific merge strategy.

   Unprefixed keys replace existing keys, like a normal merge.

   A + or >> prefix (eg: :+features, :>>desc) will concatenate the
   provided value to any existing value. :>> is used for strings,
   and :+ is used for collections.

   A & prefix is used to merge the provided map into the existing map,
   rather than replace it

   A = prefix can be used to explicitly replace an existing key, if
   there's some ambiguity with another prefix in the key's name.

   For namespaced keys, the prefix should still be part of the key *name*,
   not the namespace."
  [entity mod-map]
  (reduce-kv
    (fn [entity k v]
      (let [[modify-with actual-k] (extract-mod-and-key k)]
        (update entity actual-k
                modify-with v)))
    (or entity {})
    mod-map))

(defn merge-mods
  "Merge one or more mod maps"
  [v & vs]
  (apply merge-with concat v vs))
