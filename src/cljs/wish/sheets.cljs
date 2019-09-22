(ns ^{:author "Daniel Leong"
      :doc "sheets"}
  wish.sheets
  (:require [wish.sheets.compiler :as compiler]
            [wish.sheets.dnd5e :as dnd5e]
            [wish.sheets.dnd5e.builder :as dnd5e-builder]
            [wish.sheets.dnd5e.campaign :as dnd5e-campaign]
            [wish.sheets.dnd5e.engine :as dnd5e-engine]
            [wish.sheets.dnd5e.keymaps :as dnd5e-key]
            [wish.providers :refer [create-file-with-data
                                    error-resolver-view]]
            [wish.util :refer [click>evt <sub >evt]]
            [wish.util.nav :refer [sheet-url]]
            [wish.views.error-boundary :refer [error-boundary]]
            [wish.views.widgets :as widgets :refer [link link>evt]]))

; ======= const data =======================================

(def compiler-version 2)

; TODO we could use code splitting here to avoid loading
; sheet templates that we don't care about
(def sheets
  {:dnd5e {:name "D&D 5E"
           :fn #'dnd5e/sheet
           :builder #'dnd5e-builder/view
           :campaign #'dnd5e-campaign/view
           :v 1
           :default-sources [:wish/wdnd5e-srd]

           :engine (delay
                     (dnd5e-engine/create-engine))

           :keymaps dnd5e-key/maps}})


; ======= Public interface =================================

(defn get-engine
  [sheet-kind]
  (deref (get-in sheets [sheet-kind :engine])))

(defn get-keymaps
  [sheet-kind]
  (get-in sheets [sheet-kind :keymaps]))

(defn compile-sheet [sheet]
  (let [kind (:kind sheet)
        kind-meta (get sheets kind)]
    (when-not kind-meta
      (throw (js/Error.
               (str "Unable to get sheet meta for kind: " kind))))

    (compiler/compile-sheet
      kind-meta
      (if-let [compiler (:sheet-compiler kind-meta)]
        (compiler sheet)
        sheet))))

(defn stub-campaign
  "Create the initial data for a new campaign"
  [kind campaign-name]
  (let [kind-meta (get sheets kind)]
    (when-not kind-meta
      (throw (js/Error.
               (str "Unable to get sheet meta for kind: " kind))))

    {:v [compiler-version (:v kind-meta)]  ; wish + sheet version numbers
     :updated (.getTime (js/Date.))  ; date
     :kind kind

     :name campaign-name

     :sources (:default-sources kind-meta)

     :players #{}
     }))

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

(defn create-campaign!
  "Returns a channel that emits [err sheet-id] on success"
  [campaign-name provider-id sheet-kind]
  {:pre [(not (nil? provider-id))
         (not (nil? sheet-kind))]}
  (create-file-with-data :campaign campaign-name provider-id
                         (stub-campaign sheet-kind campaign-name)))

(defn create-sheet!
  "Returns a channel that emits [err sheet-id] on success"
  [sheet-name provider-id sheet-kind]
  {:pre [(not (nil? provider-id))
         (not (nil? sheet-kind))]}
  (create-file-with-data :sheet sheet-name provider-id
                         (stub-sheet sheet-kind sheet-name)))

; ======= Views ============================================

(defn- sheet-error-widget [what]
  (when-let [{:keys [err retry-evt]} (<sub [:sheet-error-info])]
    [:div.sheet.error
     [:p "Error loading " what]

     (if-let [data (ex-data err)]
       [error-resolver-view data]

       (if (keyword? err)
         [error-resolver-view {:state err}]

         ; unknown error; something went wrong
         [widgets/error-box err]))

     [:div
      [:a {:href "#"
           :on-click (click>evt retry-evt)}
       "Try again"]]

     [:div
      [link {:href "/sheets"}
       "Pick another sheet"]]]))

(defn sheet-loader [?sheet]
  (let [{sheet-name :name} ?sheet]
    (if-let [err-widget (sheet-error-widget (if sheet-name
                                              sheet-name
                                              "Sheet"))]
      err-widget

      (if sheet-name
        [:div (str "Loading " sheet-name "...")]
        [:div "Loading..."]))))

(defn sources-loader
  [sheet]
  (if-let [err-widget (sheet-error-widget (str "Data for " (:name sheet)))]
    err-widget

    [:div "Loading data for " (:name sheet) "..."]))

(defn sheet-unknown [kind]
  [:div (str "`" kind "`") " is not a type of sheet we know about"])

(defn- safe-sheet-content [sheet-id content]
  (try
    ; eager evaluate class, race, etc. to ensure that
    ; we can inflate everything without error
    (<sub [:all-attrs])

    ; the actual content view, wrapped in an error boundary; any
    ; errors it catches *should* be rendering-related, and not
    ; something we can do anything baout here
    [error-boundary content]

    (catch :default err
      [:div.sheet.error
       [:p "Error inflating sheet"]

       [:div
        [link {:href (sheet-url sheet-id :builder :home)}
         "Adjust sheet sources"]]

       [:div.nav-link
        [link>evt [:load-sheet! sheet-id]
         "Reload sheet"]]

       [:div
        [link {:href "/sheets"}
         "Pick another sheet"]]

       [widgets/error-box err]])))

(defn- ensuring-loaded
  [sheet-id content-fn]
  (let [sheet (<sub [:provided-sheet sheet-id])]
    (if (:sources sheet)
      (let [kind (:kind sheet)]
        (if-let [sheet-info (get sheets (keyword kind))]
          (if (<sub [:sheet-source sheet-id])
            ; sheet is ready; render!
            [error-boundary
             (content-fn sheet-info)]

            (do
              (>evt [:load-sheet-source! sheet (:sources sheet)])
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
    (fn [{view :builder}]
      [view section])))

(defn campaign
  [[campaign-id section]]
  (ensuring-loaded
    campaign-id
    (fn [{view :campaign}]
      [view section])))

(defn viewer
  [sheet-id]
  (ensuring-loaded
    sheet-id
    (fn [{view :fn}]
      [safe-sheet-content
       sheet-id
       [view]])))
