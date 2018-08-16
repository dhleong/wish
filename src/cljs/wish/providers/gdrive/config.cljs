(ns ^{:author "Daniel Leong"
      :doc "gdrive.config"}
  wish.providers.gdrive.config
  (:require [clojure.string :as str]
            [wish.providers.gdrive :as gdrive]
            [wish.providers.gdrive.styles :as styles]
            [wish.style :refer [asset]]
            [wish.util :refer [<sub]]
            [wish.views.widgets :refer [link]]))

(def ^:private src-set-kinds [["" "1x"]
                              ["@2x" "2x"]])

(defn- ->srcsets
  [base-name]
  {:src (asset (str base-name ".png"))
   :srcset (->> src-set-kinds
                (map (fn [[suffix ratio]]
                       (asset (str base-name suffix ".png " ratio))))
                (str/join ", "))})

(defn- button-variant [variant]
  (str "btn_google_signin_light_" variant "_web"))

(defn- signin-button [opts]
  [:div (merge styles/signin-button
               opts)
   [:img.normal (->srcsets (button-variant "normal"))]
   [:img.focus (->srcsets (button-variant "focus"))]
   [:img.pressed (->srcsets (button-variant "pressed"))]])

(defn signin-prompt []
  [signin-button {:on-click gdrive/signin!}])

(defn connected-view []
  (let [user (gdrive/active-user)]
    [:div "Connected!"
     (when-let [n (:name user)]
       [:div
        "Welcome back, " n])

     [:div
      [link {:href "/sheets/new"}
       "Create a new sheet"]]
     [:div
      [link {:href "/sheets"}
       "Open a sheet"]]

     [:div.button
      {:on-click gdrive/signout!}

      [:div "Disconnect Google Account"]
      (when-let [e (:email user)]
        [:div "(" e ")"])]]))

(defn view []
  (let [state (<sub [:provider-state :gdrive])]
    [:div
     [:h3 "Google Drive"]
     (case state
       :idle [:div "Preparing..."]
       :signed-out [signin-prompt]
       :signed-in [connected-view])
     ]))
