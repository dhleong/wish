(ns wish.views
  (:require
   [re-frame.core :as re-frame]
   [re-pressed.core :as rp]
   [wish.providers :as providers]
   [wish.sheets :as sheets]
   [wish.subs :as subs]
   [wish.util :refer [<sub click>evt]]
   [wish.views.error-boundary :refer [error-boundary]]
   [wish.views.home :refer [home]]
   [wish.views.new-sheet :refer [new-sheet-page]]
   [wish.views.router :refer [router]]
   [wish.views.sheet-browser :as sheet-browser]
   [wish.views.splash :as splash]
   [wish.views.update-notifier :refer [update-notifier]]
   [wish.views.widgets :refer [link] :refer-macros [icon]]
   [wish.views.widgets.media-tracker :refer [media-tracker]]
   ))

(def pages
  {:home #'home
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
        overlay-spec]]]]))

(defn main []
  [:<>
   ; we might render *slightly* differently on smartphones
   [media-tracker
    "(max-width: 479px)" [:set-device :smartphone]
    [:set-device :default]]

   [error-boundary
    [router pages]]

   [overlay]

   [update-notifier]])
