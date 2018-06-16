(ns ^{:author "Daniel Leong"
      :doc "util"}
  wish.sheets.dnd5e.util)

(defn ability->mod
  [score]
  (Math/floor (/ (- score 10) 2)))

