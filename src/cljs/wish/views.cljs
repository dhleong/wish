(ns wish.views
  (:require [archetype.views.error-boundary :refer [error-boundary]]
            [wish.providers :as providers]
            [wish.sheets :as sheets]
            [wish.util :refer [<sub click>evt]]
            [wish.views.campaign-browser :as campaign-browser]
            [wish.views.campaign.join :as join-campaign]
            [wish.views.home :refer [home]]
            [wish.views.new-campaign :as new-campaign]
            [wish.views.new-sheet :refer [new-sheet-page]]
            [wish.views.router :refer [router]]
            [wish.views.sheet-browser :as sheet-browser]
            [wish.views.splash :as splash]
            [wish.views.notifiers :refer [notifiers]]
            [wish.views.widgets :refer-macros [icon]]
            [wish.views.widgets.media-tracker :refer [media-tracker]]))

(def pages
  {:campaign #'sheets/campaign
   :campaign-browser #'campaign-browser/page
   :home #'home
   :join-campaign #'join-campaign/page
   :new-campaign #'new-campaign/page
   :new-sheet #'new-sheet-page
   :sheet #'sheets/viewer
   :sheet-browser #'sheet-browser/page
   :sheet-builder #'sheets/builder
   :splash #'splash/page
   :provider-config #'providers/config-view
   })

(defn overlay []
  (when-let [[overlay-class overlay-spec] (<sub [:showing-overlay])]
    [:div#overlay-container
     {:on-click (click>evt [:toggle-overlay])}

     [:div
      {:id overlay-class
       :on-click (fn [e]
                   ; prevent click propagation by default
                   ; to avoid the event leaking through and
                   ; triggering the dismiss click on the bg
                   (.stopPropagation e))}
      [:div.close-button
       {:on-click (click>evt [:toggle-overlay])}
       (icon :close)]

      ; finally, the overlay itself
      [:div.scroll-host
       [:div.wrapper
        [error-boundary
         overlay-spec]]]]]))

(defn main []
  [:<>
   ; we might render *slightly* differently on smartphones
   [media-tracker
    "(max-width: 479px)" [:set-device :smartphone]
    [:set-device :default]]

   [media-tracker
    "(hover: none) and (pointer: coarse)" [:set-touch true]
    "(hover: none) and (pointer: fine)" [:set-touch true]
    [:set-touch false]]

   [error-boundary
    [router pages]]

   [overlay]

   [notifiers]])
