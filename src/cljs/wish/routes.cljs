(ns wish.routes
  (:require-macros [secretary.core :refer [defroute]])
  (:require [pushy.core :as pushy]
            [wish.util.nav :as nav :refer [hook-browser-navigation!
                                           navigate!]]))

(defn app-routes []
  (nav/init!)

  ;; --------------------
  ;; define routes here
  (defroute "/" []
    (navigate! :home))

  (defroute "/campaigns" []
    (navigate! :campaign-browser))

  (defroute "/campaigns/new" []
    (navigate! :new-campaign))

  (defroute #"/campaigns/([a-z0-9-]+/[^/]+)" [id]
    (navigate! :campaign [(keyword id)]))

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

  ;; --------------------
  (hook-browser-navigation!))

