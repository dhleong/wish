(ns ^{:author "Daniel Leong"
      :doc "new-campaign"}
  wish.views.new-campaign
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [wish.util :refer [fn-click]]
                   [wish.util.log :as log])
  (:require [clojure.core.async :refer [<!]]
            [clojure.string :as str]
            [reagent.core :as r]
            [reagent-forms.core :refer [bind-fields]]
            [wish.providers :as providers]
            [wish.sheets :as sheets]
            [wish.util :refer [<sub]]
            [wish.util.nav :as nav :refer [campaign-url]]
            [wish.views.widgets :refer [icon link]]))

(defn- do-create [form]
  (sheets/create-campaign!
    (:name form)
    (:provider form)
    (:sheet form)))

(defn- on-create-success [dm-id]
  (log/info "Created campaign with id " dm-id " successfully")
  (nav/replace! (campaign-url dm-id)))

(defn- can-submit? [state {:keys [name provider sheet] :as form}]
  (println "can-submit? " state form)
  (not (or (not= :idle state)
           (ex-message state)
           (str/blank? name)
           (nil? sheet)
           (= :-none sheet)
           (nil? provider))))

(defn page []
  (r/with-let [form-data (r/atom {:name "My New Campaign"})
               state (r/atom :idle)]
    [:div
     [:h3
      [link {:href "/"}
       (icon :home)]
      "New Campaign"]

     [:div
      [:form#new-campaign
       {:on-submit (fn-click
                     (reset! state :creating)
                     (go (let [[err dm-id] (<! (do-create @form-data))]
                           (if err
                             (reset! state err)
                             (on-create-success dm-id)))))}

       [bind-fields
        [:<>
         [:div
          [:p "Pick a name (you can change it later)"]
          [:input {:field :text
                   :id :name}]]

         [:div
          [:p "Pick a sheet type (you " [:i "can't"] " can't change this one!)"]
          (into
            [:select {:field :list
                      :id :sheet}
             [:option {:key :-none} "—Pick a sheet type—"]]

            (for [[sheet-id info] sheets/sheets]
              ^{:key sheet-id}
              [:option {:key sheet-id}
               (:name info)]))]]

        form-data]

       [:div
        [:p "Where do you want to store it?"]

        (for [[provider-id state] (<sub [:storable-provider-states])]
          ^{:key provider-id}
          [bind-fields
           [:div
            [:input {:value provider-id
                     :id provider-id
                     :name :provider
                     :field :radio
                     :disabled (not= state :ready)}]
            [:label {:for provider-id}
             (:name (providers/get-info provider-id))]
            (when (not= state :ready)
              [link {:href (str "/providers/" (name provider-id) "/config")}
               "Configure"])]

           form-data])]

       [:div
        [:input {:type :submit
                 :value "Create Campaign"
                 :disabled (not (can-submit? @state @form-data))}]] ]]]))
