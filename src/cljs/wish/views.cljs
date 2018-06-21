(ns wish.views
  (:require
   [re-frame.core :as re-frame]
   [re-pressed.core :as rp]
   [wish.providers :as providers]
   [wish.sheets :as sheets]
   [wish.subs :as subs]
   [wish.util :refer [<sub]]
   [wish.views.home :refer [home]]
   [wish.views.new-sheet :refer [new-sheet-page]]
   [wish.views.router :refer [router]]
   [wish.views.widgets :refer [link]]
   ))

(def pages
  {:home #'home
   :new-sheet #'new-sheet-page
   :sheet #'sheets/viewer
   :provider-config #'providers/config-view
   })

(defn main []
  [:div#main
   [router pages]])
