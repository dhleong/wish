(ns wish.views
  (:require
   [re-frame.core :as re-frame]
   [re-pressed.core :as rp]
   [wish.subs :as subs]
   [wish.util :refer [<sub]]
   [wish.views.home :refer [home]]
   [wish.views.router :refer [router]]
   [wish.views.widgets :refer [link]]
   [wish.sheets :as sheets]
   ))

(def pages
  {:home #'home
   :sheet #'sheets/viewer
   })

(defn main []
  [:div#main
   [router pages]])
