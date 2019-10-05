(ns wish.routes
  (:require-macros [secretary.core :refer [defroute]])
  (:require [secretary.core :as secretary]
            [wish.util.nav :as nav :refer [hook-browser-navigation!
                                           navigate!]]))

(defn- def-routes []
  (secretary/reset-routes!)

  ;;
  ;; app routes declared here:

  (defroute "/" []
    (navigate! :home))

  (defroute "/campaigns" []
    (navigate! :campaign-browser))

  (defroute "/campaigns/new" []
    (navigate! :new-campaign))

  (defroute #"/campaigns/([a-z0-9-]+/[^/]+)[/]?" [id]
    (navigate! :campaign [(keyword id)]))

  (defroute #"/campaigns/([a-z0-9-]+/[^/]+)/([^/]+)" [id section]
    (navigate! :campaign [(keyword id) (keyword section)]))

  (defroute #"/join-campaign/([a-z0-9-]+/[^/]+)(/n/[^/]+)?/as/(.*)" [campaign-id label sheet-id]
    (navigate! :join-campaign [(keyword campaign-id)
                               (keyword sheet-id)
                               (when-not (empty? label)
                                 (js/decodeURIComponent
                                   ; trim off /n/
                                   (subs label 3)))]))

  (defroute "/sheets" []
    (navigate! :sheet-browser))

  (defroute "/sheets/new" []
    (navigate! :new-sheet))

  (defroute #"/sheets/([a-z0-9-]+/[^/]+)" [id]
    (navigate! :sheet (keyword id)))

  (defroute #"/sheets/([a-z0-9-]+/[^/]+)/builder" [id]
    (navigate! :sheet-builder [(keyword id)]))

  (defroute #"/sheets/([a-z0-9-]+/[^/]+)/builder/(.*)" [id section]
    (navigate! :sheet-builder [(keyword id) (keyword section)]))

  (defroute "/providers/:provider-id/config" [provider-id]
    (navigate! :provider-config (keyword provider-id)))
  )

(defn app-routes []
  (nav/init!)

  (def-routes)

  (hook-browser-navigation!))

