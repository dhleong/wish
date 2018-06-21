(ns ^{:author "Daniel Leong"
      :doc "new-sheet"}
  wish.views.new-sheet
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [clojure.core.async :refer [chan <!]]
            [clojure.string :as str]
            [reagent.core :as r]
            [wish.providers :as providers]
            [wish.sheets :as sheets]
            [wish.util :refer [<sub]]
            [wish.util.nav :as nav]
            [wish.views.widgets :refer [link]]))

(defn change->
  "Creates an :on-change handler that assoc's the
   new value to the element's id in the provided ratom.
   You can also provide a custom key-fn and val-fn that
   receive the target element, if you prefer"
  ([ratom]
   (change-> ratom
             #(keyword (.-id %))
             #(.-value %)))
  ([ratom key-fn val-fn]
   (fn [e]
     (.preventDefault e)
     (swap! ratom
            assoc
            (key-fn (.-target e))
            (val-fn (.-target e))))))

(defn radio->
  "Creates an :on-change handler for a radio-button
   that stores the id of the element in the provided ratom"
  [ratom]
  (change-> ratom
            #(keyword (.-name %))
            #(keyword (.-id %))))

(defn- do-create
  [data]
  (providers/create-sheet!
    (:name data)
    (:provider data)
    (:kind data)))

(defn on-create-success [sheet-id]
  (println "Created sheet with id " sheet-id " successfully")
  (nav/replace! (str "/sheets/" (namespace sheet-id)
                     "/" (name sheet-id) "/builder")))

(defn new-sheet-page []
  (let [form-data (r/atom {:name "My New Character"})
        state (r/atom :idle)]
    (fn []
      (let [data @form-data
            current-name (:name data)
            selected-sheet (:sheet data)
            selected-provider (:provider data)]
        [:div
         [:h3 "New Sheet"]
         [:div
          [:form#new-sheet
           {:on-submit (fn [e]
                         (.preventDefault e)
                         (reset! state :creating)
                         (go (let [[err sheet-id] (<! (do-create @form-data))]
                               (if err
                                 (reset! state err)
                                 (on-create-success sheet-id)))))}

           [:div
            [:p "Pick a name (you can change it later)"]
            [:input#name {:type 'text
                          :autoComplete 'off
                          :on-change (change-> form-data)
                          :value current-name}]]

           [:div
            [:p "Pick a sheet type (you can't change this one!)"]
            (for [[sheet-id info] sheets/sheets]
              ^{:key sheet-id}
              [:div
               [:input {:id sheet-id
                        :name 'sheet
                        :type 'radio
                        :on-change (radio-> form-data)
                        :checked (= sheet-id selected-sheet)}]
               [:label {:for sheet-id}
                (:name info)]])]

           [:div
            [:p "Where do you want to store it?"]

            (for [[provider-id state] (<sub [:provider-states])]
              ^{:key provider-id}
              [:div
               [:input {:id provider-id
                        :name 'provider
                        :type 'radio
                        :on-change (radio-> form-data)
                        :checked (= provider-id selected-provider)
                        :disabled (not= state :signed-in)}]
               [:label {:for provider-id}
                (:name (providers/get-info provider-id))]
               (when (= state :signed-out)
                 [link {:href (str "/providers/" (name provider-id) "/config")}
                  "Configure"])])]

           [:div
            [:input#submit {:type "submit"
                            :value "Build Character"
                            :disabled (or (and (not= :idle @state)
                                               (not (.-message @state)))
                                          (str/blank? current-name)
                                          (nil? selected-sheet)
                                          (nil? selected-provider))}]]]]]))))
