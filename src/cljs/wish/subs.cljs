(ns wish.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [wish.db :as db]
            [wish.subs-util :refer [active-sheet-id]]
            [wish.sheets :as sheets]
            [wish.sources.compiler :refer [inflate]]
            [wish.sources.core :as src :refer [find-class find-race]]))

; ======= Provider-related =================================

(reg-sub :provider-states :provider-states)
(reg-sub
  :provider-state
  :<- [:provider-states]
  (fn [states [_ provider-id]]
    (when-not provider-id
      (js/console.warn "[subs] nil provider-id provided to :provider-state"))
    (get states provider-id :idle)))

(reg-sub
  :providers-listing?
  (fn [db _]
    (seq (:providers-listing db))))


; ======= Sheet-related ====================================

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
(reg-sheet-sub :sheet-kind :kind)
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

(reg-sub
  :known-sheets
  :<- [:sheets]
  (fn [sheets _]
    (->> sheets
         (map (fn [[id v]]
                (assoc v :id id)))
         (filter :name)
         (sort-by :name))))

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
  :<- [:sheet-kind]
  :<- [:sheet-source]
  :<- [:options]
  :<- [:class-metas]
  (fn [[sheet-kind source options metas] _]
    (when source
      (->> metas
           (map (fn [m]
                  (merge m (find-class source (:id m)))))
           (map (fn [c]
                  (-> c
                      (inflate source options)
                      (sheets/post-process
                        sheet-kind
                        source
                        :class)))))
      )))

(reg-sub
  :races
  :<- [:sheet-meta]
  :<- [:sheet-source]
  :<- [:options]
  :<- [:race-ids]
  (fn [[sheet-meta source options ids] _]
    (when source
      (->> ids
           (map (partial find-race source))
           (map (fn [r]
                  (-> r
                      (inflate source options)
                      (sheets/post-process
                        (:kind sheet-meta)
                        source
                        :race))))))))

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

(reg-sub
  :limited-uses-map
  :<- [:limited-uses]
  (fn [limited-uses]
    (reduce
      (fn [m v]
        (assoc m (:id v) v))
      {}
      limited-uses)))


; ======= character builder-related ========================

(reg-sub
  :available-entities
  :<- [:sheet-source]
  (fn [source [_ entity-kind]]
    (src/list-entities source entity-kind)))


; ======= Save state =======================================

(reg-sub
  :save-state
  (fn [{::db/keys [pending-saves processing-saves]}]
    (cond
      ; if there are any processing, show :saving state
      (not (empty? processing-saves)) :saving

      ; nothing processing, but some pending
      (not (empty? pending-saves)) :pending

      ; otherwise, idle
      :else :idle)))
