(ns ^{:author "Daniel Leong"
      :doc "Utils for generating hiccup from formatted text"}
  wish.util.formatted
  (:require [clojure.string :as str]))

(def ul-regex #"^[ ]*-[ ]*(.*)$")
(def ol-regex #"^[ ]*[0-9]\.[ ]*(.*)$")

(defn- maybe-span
  "If there's just one part, return it
  directly; if there are more, wrap them
  in a :span"
  [& parts]
  (let [parts (remove empty? parts)]
    (case (count parts)
      0 nil
      1 (first parts)
      (if (= :span (ffirst parts))
        ; expand an existing span to include the other parts
        (apply conj parts)

        ; wrap
        (into [:span]
              parts)))))

(defn- collapse-into
  "If `parts` is a container like :span, collapse
  its contents into `spec`. Otherwise, just create
  a normal vector of [spec parts]"
  [spec parts]
  (if (= :span (first parts))
    (assoc parts 0 spec)
    [spec parts]))

(defn- collapse-list-type
  [result item container-spec]
  {:pre [(or (= :ul container-spec)
             (= :ol container-spec))]}
  (if (= container-spec (-> result last first))
    ; join with previous
    (let [ul-items (-> result last rest)]
      (concat (drop-last result)
              [(vec
                 (concat [container-spec]
                         ul-items
                         [item]))]))
    ; create a new ul to contain it
    (concat result [[container-spec item]])))

(defn- collapse-lists
  [result item]
  (cond
    (= :li.ul (first item))
    (collapse-list-type result item :ul)

    (= :li.ol (first item))
    (collapse-list-type result item :ol)

    ; otherwise, let it be
    :else (concat result [item])))

(defn- conj-in
  "(conj) in a nested structure with a depth based
   on the stack"
  [coll stack value]
  (letfn [(conj-path [coll stack-height base-path]
            (if (<= stack-height 0)
              base-path
              (recur
                (last coll)
                (dec stack-height)
                (conj base-path (dec (count coll))))))]
    (let [stack-height (count stack)
          path (conj-path coll stack-height [])]
      (if (and (seq coll)
               (seq path))
        (update-in coll path conj value)
        (conj coll value)))))

(defn- expand-formats
  "Uses a tokenizer approach to expanding possibly-nested ** and _
   text wrappings into [:b] and [:i], respectively. Should be slightly
   more efficient than the recursive regex thing we were doing previously
   for just `**`, while also adding support the `_` tag. Basically trading
   more memory use (maintaining the token stack, plus many small allocations
   when calculating the path in which to conj string parts) for CPU time,
   since this is a linear sweep with a single regex instead of multiple
   recursive regex calls (which may also have made small allocations, so
   hopefully that balances out)."
  [line]
  ; NOTE this assumes reasonably well-formatted input
  (loop [result []
         stack []
         tokens (str/split line #"(\*\*|_)")]
    (if-let [tok (first tokens)]
      (let [tok (case tok
                  "**" :b
                  "_" :i
                  tok)
            stack-top (last stack)]
        (cond
          (= tok stack-top)
          (recur
            result
            (pop stack)
            (next tokens))

          (keyword? tok)
          (recur
            (conj-in result stack [tok])
            (conj stack tok)
            (next tokens))

          :else
          (recur
            (if (empty? tok)
              result
              (conj-in result stack tok))

            stack
            (next tokens))))

      ; done!
      result)))

(defn- wrap-line
  [line]
  ; a cond-let macro would be wonderful...
  (if-let [li (re-find ul-regex line)]
    (collapse-into :li.ul (wrap-line (second li)))
    (if-let [li (re-find ol-regex line)]
      (collapse-into :li.ol (wrap-line (second li)))

      ;; else, just expand ** and _ wraps
      (apply maybe-span
             (expand-formats line)))))

(declare ->hiccup)

(defn- format-row
  [element-type row]
  (->> row
       (map (fn [item]
              (->> (->hiccup item)
                   (into element-type))))
       (into [:tr])))

(defn ->hiccup
  [text]
  (when text
    (cond
      ; base case: formatted strings
      (string? text)
      (let [lines (str/split text "\n")]
        (->> lines
             (map wrap-line)
             (reduce collapse-lists [])))

      ; tables
      (and (map? text)
           (or (:rows text)
               (:headers text)))
      (->> [:table
            (when-let [headers (:headers text)]
              [:thead
               (format-row [:th] headers)])

            (when-let [rows (:rows text)]
              (->> rows
                   (map (partial format-row [:td]))
                   (into [:tbody])))
            ]
           (filterv identity)

           ; wrap in an outer sequence
           (vector))

      ; sequence of entries (can be any of the above)
      (vector? text)
      (->> text
           (mapcat ->hiccup))

      :else
      (throw (js/Error. (str "Unknown format:\n" text))))))
