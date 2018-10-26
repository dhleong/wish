(ns ^{:author "Daniel Leong"
      :doc "Push-notification API"}
  wish.push
  (:require [wish.config :as config]
            [wish.providers :as providers]
            [wish.util.http :refer [POST]]))

(def push-server-version "v1")

(def ^:private push-url-base (str config/push-server "/" push-server-version))

(defn create-session [interested-ids]
  (when-let [auth (providers/watch-auth-map interested-ids)]
    (POST (str push-url-base "/push/sessions")
          {:auth auth

           ; default serialization of a keyword drops the namespace
           :ids (map (fn [id]
                       (subs (str id) 1))
                     interested-ids)})))
