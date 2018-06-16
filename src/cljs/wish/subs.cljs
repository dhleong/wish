(ns wish.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [wish.sources.compiler :refer [apply-options]]
            [wish.sources.core :refer [find-class find-race]]))

(defn active-sheet-id
  [db & [page-vec]]
  (let [page-vec (or page-vec
                     (:page db))]
    (let [[page args] page-vec]
      (when (= :sheet page)
        ; NOTE: the first arg is the sheet kind;
        ; the second is the id
        (second args)))))

(defn reg-sheet-sub
  [id getter]
  ; NOTE: instead of depending on a single subscription,
  ; we go ahead and create a separate subscription for
  ; each part of the sheet, to avoid a small edit to HP,
  ; for example, causing all of the spell lists and features
  ; (which rely on classes, etc.) to be re-calculated
  (reg-sub
    id
    :<- [:sheet-meta]
    (fn [sheet _]
      (getter sheet))))

(reg-sub :page :page)
(reg-sub :sheets :sheets)
(reg-sub :sheet-sources :sheet-sources)

(reg-sheet-sub :sheet :sheet)
(reg-sheet-sub :class-metas (comp vals :classes))
(reg-sheet-sub :race-ids :races)
(reg-sheet-sub :limited-used :limited-uses)
(reg-sheet-sub :options :options)

(reg-sub
  :active-sheet-id
  :<- [:page]
  (fn [page-vec _]
    (active-sheet-id nil page-vec)))

(reg-sub
  :provided-sheet
  :<- [:sheets]
  (fn [sheets [_ sheet-id]]
    (get sheets sheet-id)))

; if a specific sheet-id is not provided, loads
; for the active sheet id
(reg-sub
  :sheet-source
  :<- [:sheet-sources]
  :<- [:active-sheet-id]
  (fn [[sources active-id] [_ sheet-id]]
    (let [{:keys [source loaded?]} (get sources (or sheet-id
                                                    active-id))]
      (when loaded?
        source))))


; ======= Accessors for the active sheet ===================

(reg-sub
  :sheet-meta
  :<- [:sheets]
  :<- [:active-sheet-id]
  (fn [[sheets id]]
    (get sheets id)))

(reg-sub
  :classes
  :<- [:sheet-source]
  :<- [:options]
  :<- [:class-metas]
  (fn [[source options metas] _]
    (when source
      (->> metas
           (map (fn [m]
                  (merge m (find-class source (:id m)))))
           (map (fn [c]
                  (apply-options source c options))))
      )))

(reg-sub
  :races
  :<- [:sheet-source]
  :<- [:race-ids]
  (fn [[source ids] _]
    (when source
      (map (partial find-race source)
           ids))))

; semantic convenience for single-race systems
(reg-sub
  :race
  :<- [:races]
  (fn [races _]
    (first races)))

(defn- uses-with-context
  [kind entity]
  (->> entity
       :limited-uses
       vals
       (map (fn [item]
              (assoc item
                     :wish/context-type kind
                     :wish/context entity)))))

(reg-sub
  :limited-uses
  :<- [:classes]
  :<- [:races]
  ; TODO also, probably, items?
  (fn [[classes races]]
    (flatten
      (concat
        (->> races
             (map (partial uses-with-context :race)))
        (->> classes
             (map (partial uses-with-context :class)))))))
