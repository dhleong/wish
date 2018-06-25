(ns ^{:author "Daniel Leong"
      :doc "dev-server"}
  wish.dev-server
  (:require [clojure.java.io :as io]
            [compojure.route :as route]
            [compojure.core :refer [GET ANY defroutes]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.response :as response]))

(defroutes routes
  (route/resources "/" {:root "public"})
  (GET "/" [] (-> (response/resource-response "index.html" {:root "public"})
                  (response/content-type "text/html")))

  ; fallback handler for custom 404 page
  (ANY "*" [] (-> (response/resource-response "404.html" {:root "public"})
                  (response/content-type "text/html"))))

(def http-handler
  (-> routes
      (wrap-defaults site-defaults)))
