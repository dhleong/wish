(ns ^{:author "Daniel Leong"
      :doc "overlays.custom-item"}
  wish.sheets.dnd5e.overlays.custom-item
  (:require-macros [wish.util.log :refer [log]])
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [reagent-forms.core :refer [bind-fields]]
            [wish.inventory :as inv]
            [wish.sheets.dnd5e.data :as data]
            [wish.sheets.dnd5e.style :as styles]
            [wish.util :refer [>evt fn-click]]))


; ======= utils ===========================================

(defn- for-type
  "creates a :visible? fn that returns true when
   the selected :type is equal to the provided key"
  [type]
  (comp (partial = type) :type))

(defn- for-types
  "As with `for-type`, but for *any* of the provided types"
  [& types]
  (comp (set types) :type))

; NOTE public for testing!
(defn install-limited-use
  "Modify the custom item spec to install the limited use directives
   if necessary, and remove any related keys as necessary"
  [{:keys [limited-use?]
    item-id :id
    item-name :name
    {use-name :name
     uses :uses} :limited-use
    :as item}]
  (let [item (if limited-use?
               (assoc item
                      :! [[:!add-limited-use
                           {:id item-id
                            :name (or use-name item-name)
                            :uses (or uses 1)
                            :restore-trigger :long-rest}]])

               ; strip :attunes? if there's no limited-use
               (dissoc item :attunes?))]

    ; always strip these keys
    (dissoc item :limited-use? :limited-use)))


; ======= internal widgets ================================

(defn- kind-selector
  "Returns a section for selecting the :kind for the
   item's current type"
  [type values-fn]
  [:div.section {:field :container
                 :visible? (for-type type)}
   [:label {:for [:kind type]}
    "Kind\u00A0"]
   [:select {:field :list
             :id [:kind type]}
    (for [{:keys [id label]} (values-fn)]
      [:option {:key id}
       label])]] )

(defn- item-creator-basics []
  [:<>
   [:div.section
    [:input {:field :text
             :id :name
             :placeholder "Name"
             :auto-focus true
             :auto-complete "off"}]
    [:div.error {:field :alert
                 :id :errors.name}]]

   [:div.section
    [:label {:for :type}
     "Type\u00A0"]
    [:select {:field :list
              :id :type}
     [:option {:key :ammunition} "Ammunition"]
     [:option {:key :armor} "Armor"]
     [:option {:key :gear} "Gear"]
     [:option {:key :weapon} "Weapon"]
     [:option {:key :potion} "Potion"]
     [:option {:key :other} "Other"]]]])

(defn- item-creator-ammunition-config [for-type]
  [:div.section {:field :container
                 :visible? (for-type :ammunition)}
   [:input {:field :fast-numeric
            :id :default-quantity
            :placeholder "Default Quantity"}]
   [:div.error {:field :alert
                :id :errors.default-quantity}]])

(defn- item-creator-bonuses [for-types]
  [:div.section.flex
   [:div {:field :container
          :visible? (for-types :ammunition
                               :weapon
                               :armor)}
    [:label {:for :+}
     "+\u00A0"]
    [:input.numeric {:field :fast-numeric
                     :id :+}]]

   [:div {:field :container
          :visible? (for-types :gear
                               :weapon
                               :armor)}
    [:input {:field :checkbox
             :id :limited-use?}]
    [:label.meta {:for :limited-use?}
     "Provides a Limited-Use"]]])

(defn- item-creator-limited-use []
  [:div.section.limited-use {:field :container
                             :visible? #(:limited-use? %)}
   [:div
    [:input {:field :checkbox
             :id :attunes?}]
    [:label.meta {:for :attunes?}
     "Requires attunement"]]

   [:div
    [:input {:field :text
             :id :limited-use.name
             :placeholder "Limited Use Name"}]]

   [:div
    [:label.meta {:for :attunes?}
     "Uses\u00A0"]
    [:input.numeric {:field :fast-numeric
                     :id :limited-use.uses}]]])


; ======= reagent-forms events ============================

(defn- event-clear-errors [id value doc]
  ; when we provide a value for :name, :default-quantity,
  ; clear any errors that had been raised about them
  (when (and (some (set id) [:name
                             :default-quantity])
             (not (str/blank? value)))
    (update doc :errors dissoc (first id))))


; ======= creation ========================================

(defn- save!
  "Given the current, validated state *map* (not the atom)
   perform the item creation"
  [s]
  (let [editing-existing? (:id s)

        custom-id (or (:id s)

                      ; generate a custom id if none was provied
                      (inv/custom-id
                        (:name s)))
        s (-> s
              (dissoc :errors)
              (assoc :id custom-id)

              ; create any requested limited-use directive
              install-limited-use

              ; pull up the appropriate kind
              (assoc :kind (get-in s [:kind (:type s)]))

              ; and inflate
              data/inflate-by-type)]

    (if editing-existing?
      (do
        (log "Edit! custom item" s)
        (>evt [:inventory-edit s]))

      (do
        (log "Add! custom item" s)
        (>evt [:inventory-add s])))
    (>evt [:toggle-overlay nil])))

(defn- try-save!
  "Given the state atom, validate fields that need to be
   validated and, if everything looks good, pass the current
   state map to save!"
  [state]
  (let [s @state]
    (cond
      (str/blank? (:name s))
      (swap! state assoc-in [:errors :name]
             "Name must not be blank")

      (and (= :ammunition (:type s))
           (or (str/blank? (:default-quantity s))
               (<= (:default-quantity s)
                   0)))
      (swap! state assoc-in [:errors :default-quantity]
             "You must provide a quantity > 0")

      :else (save! s))))


; ======= Actual overlay ==================================

(defn custom-item-overlay
  ([] (custom-item-overlay nil))
  ([existing-item]
   (r/with-let [state (r/atom (or existing-item
                                  {:type :other}))]

     [:div styles/custom-item-overlay
      [:h5 "Custom Item"]
      [bind-fields
       [:form {:on-submit (fn-click
                            (try-save! state))}

        ; name and type
        (item-creator-basics)

        ; ammunition config
        (item-creator-ammunition-config for-type)

        ; armor config
        (kind-selector :armor data/armor-kinds)

        ; weapon config
        (kind-selector :weapon data/weapon-kinds)

        ; weapon + amount / limited use toggle
        (item-creator-bonuses for-types)

        ; limited-use config
        (item-creator-limited-use)

        ; description
        [:div.section
         [:textarea.stretch {:field :textarea
                             :id :desc
                             :placeholder "Description (optional)"
                             :rows 4}]]

        ; submit button
        [:div.section
         [:input {:type :submit
                  :value (if existing-item
                           "Save!"
                           "Create!")}]]]

       ; the state atom
       state

       ; events:
       event-clear-errors]])))
