(ns ^{:author "Daniel Leong"
      :doc "Utils for generating formatted hiccup from text"}
  wish.util.formatted
  (:require [clojure.string :as str]))

(def ul-regex #"^[ ]*-[ ]*(.*)$")
(def ol-regex #"^[ ]*[0-9]\.[ ]*(.*)$")
(def b-regex #"^(.*)\*\*(.*)\*\*(.*)$")

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
        (vec
          (cons :span
                parts))))))

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

(defn- wrap-line
  [line]
  ; a cond-let macro would be wonderful...
  (if-let [li (re-find ul-regex line)]
    (collapse-into :li.ul (wrap-line (second li)))
    (if-let [li (re-find ol-regex line)]
      (collapse-into :li.ol (wrap-line (second li)))
      (if-let [[_ before b after] (re-find b-regex line)]
        (maybe-span
          (wrap-line before)
          [:b b]
          (wrap-line after))

        ; nothing left to do:
        line))))


(defn ->hiccup
  [text]
  (let [lines (str/split text "\n")]
    (->> lines
         (map wrap-line)
         (reduce collapse-lists []))))
