(ns ^{:author "Daniel Leong"
      :doc "sheets"}
  wish.sheets
  (:require [wish.sheets.dnd5e :as dnd5e]
            [wish.sheets.dnd5e.builder :as dnd5e-builder]
            [wish.sheets.dnd5e.util :as dnd5e-util]
            [wish.sources.compiler :refer [compiler-version]]
            [wish.providers :refer [create-sheet-with-data]]
            [wish.util :refer [click>evt <sub >evt]]))

; ======= const data =======================================

; TODO we could use code splitting here to avoid loading
; sheet templates that we don't care about
(def sheets
  {:dnd5e {:name "D&D 5E"
           :fn #'dnd5e/sheet
           :builder #'dnd5e-builder/view
           :v 1
           :default-sources [:wish/wdnd5e-srd]

           ; Function for post-processing entities,
           ;  IE: applying :attr side-effects.
           ; post-process functions should accept
           ;  [entity, data-source, entity-kind]
           :post-process dnd5e-util/post-process
           }})


; ======= Public interface =================================

(defn post-process
  "Apply sheet-kind-specific post-processing to an entity"
  [entity sheet-kind data-source entity-kind]
  (if-let [processor (get-in sheets [sheet-kind :post-process])]
    (processor entity data-source entity-kind)

    ; no processor for this sheet; pass through
    entity))

(defn stub-sheet
  "Create the initial data for a new sheet"
  [kind sheet-name]
  (let [kind-meta (get sheets kind)]
    (when-not kind-meta
      (throw (js/Error.
               (str "Unable to get sheet meta for kind: " kind))))

    {:v [compiler-version (:v kind-meta)]  ; wish + sheet version numbers
     :updated (.getTime (js/Date.))  ; date
     :kind kind

     :name sheet-name

     :sources (:default-sources kind-meta)

     :classes {}
     :races []

     :inventory {}
     :items {}
     :equipped #{}
     }))

(defn create-sheet!
  "Returns a channel that emits [err sheet-id] on success"
  [sheet-name provider-id sheet-kind]
  {:pre [(not (nil? provider-id))
         (not (nil? sheet-kind))]}
  (create-sheet-with-data sheet-name provider-id
                          (stub-sheet sheet-kind sheet-name)))

; ======= Views ============================================

(defn sheet-error-widget []
  (when-let [{:keys [err retry-evt]} (<sub [:sheet-error-info])]
    [:div.sheet.error
     [:p "Error loading sheet"]
     [:div "Info for nerds: "
      [:p.error-info (.-message err)]]
     [:div
      [:a {:href "#"
           :on-click (click>evt retry-evt)}
       "Try again"]]]))

(defn sheet-loader [?sheet]
  [:<>
   (if-let [{:keys [name]} ?sheet]
     [:div (str "Loading " name "...")]
     [:div "Loading..."])
   [sheet-error-widget]])

(defn sources-loader
  [sheet]
  [:<>
   [:div "Loading data for " (:name sheet) "..."]
   [sheet-error-widget]])

(defn sheet-unknown [kind]
  [:div (str "`" kind "`") " is not a type of sheet we know about"])

(defn- ensuring-loaded
  [sheet-id content-fn]
  (let [sheet (<sub [:provided-sheet sheet-id])]
    (if (:sources sheet)
      (let [kind (:kind sheet)]
        (if-let [sheet-info (get sheets (keyword kind))]
          (if-let [source (<sub [:sheet-source sheet-id])]
            ; sheet is ready; render!
            (content-fn sheet-info)

            (do
              (>evt [:load-sheet-source! sheet-id (:sources sheet)])
              [sources-loader sheet]))

          ; unknown sheet kind
          [sheet-unknown kind]))

      ; either we don't have the sheet at all, or it's just
      ; a stub with no actual data; either way, load it!
      (do
        (>evt [:load-sheet! sheet-id])
        [sheet-loader sheet]))))

(defn builder
  [[sheet-id section]]
  (ensuring-loaded
    sheet-id
    (fn [sheet-info]
      [(:builder sheet-info) section])))

(defn viewer
  [sheet-id]
  (ensuring-loaded
    sheet-id
    (fn [sheet-info]
      [(:fn sheet-info)])))
