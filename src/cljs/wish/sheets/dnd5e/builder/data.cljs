(ns ^{:author "Daniel Leong"
      :doc "Builder-specific data"}
  wish.sheets.dnd5e.builder.data)

; ======= Ability score generation ========================

(def point-buy-max 27)

(def score-point-cost {8 0
                       9 1
                       10 2
                       11 3
                       12 4
                       13 5
                       14 7
                       15 9})

