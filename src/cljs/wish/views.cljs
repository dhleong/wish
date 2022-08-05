(ns wish.views
  (:require [wish.providers :as providers]
            [wish.sheets :as sheets]
            [wish.views.campaign-browser :as campaign-browser]
            [wish.views.campaign.join :as join-campaign]
            [wish.views.home :refer [home]]
            [wish.views.new-campaign :as new-campaign]
            [wish.views.new-sheet :refer [new-sheet-page]]
            [wish.views.overlay :refer [overlay]]
            [wish.views.router :refer [router]]
            [wish.views.sheet-browser :as sheet-browser]
            [wish.views.splash :as splash]
            [wish.views.notifiers :refer [notifiers]]
            [wish.views.widgets.error-boundary :refer [error-boundary]]
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
