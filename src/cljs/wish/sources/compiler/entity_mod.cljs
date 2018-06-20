(ns ^{:author "Daniel Leong"
      :doc "Utilities for data-based entity modification"}
  wish.sources.compiler.entity-mod)

(defn- mod-key
  ([k skip-chars]
   (let [n (name k)]
     (keyword (namespace k) (subs n skip-chars)))))

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
             [(fn [features new-features]
                (reduce-kv
                  (fn [features-map fk new-feature]
                    (cond
                      (keyword? new-feature)
                      (assoc features-map new-feature true)

                      (map? new-feature)
                      (assoc features-map (:id new-feature) new-feature)))
                  features
                  new-features))
              the-key]

             ; normal case
             [concat the-key]))
      \> (if (not= \> (second n))
           (throw (js/Error. (str "Invalid mod prefix on key " k)))
           [str (mod-key k 2)])
      \& [merge (mod-key k 1)]

      ; otherwise, just replace:
      [(fn [a b] b) k])))

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

   For namespaced keys, the prefix should still be part of the key *name*,
   not the namespace."
  [entity mod-map]
  (reduce-kv
    (fn [entity k v]
      (let [[modify-with actual-k] (extract-mod-and-key k)]
        (update entity actual-k
                modify-with v)))
    entity
    mod-map))
