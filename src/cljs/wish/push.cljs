(ns ^{:author "Daniel Leong"
      :doc "Push-notification API"}
  wish.push
  (:require-macros [cljs.core.async :refer [go]]
                   [wish.util.log :refer [log] :as log])
  (:require [clojure.core.async :refer [<!]]
            [wish.config :as config]
            [wish.providers :as providers]
            [wish.sheets.util :refer [unpack-id]]
            [wish.util :refer [>evt]]
            [wish.util.http :refer [POST]]))

(def push-server-version "v1")

(def ^:private push-url-base (str config/push-server "/" push-server-version "/push"))

(defn session-args [auth interested-ids]
  {:auth auth

   :ids (->> interested-ids

             ; remove sheet ids for which we don't have auth
             (filter (fn [sheet-id]
                       (let [[provider-id _] (unpack-id sheet-id)]
                         (contains? auth provider-id))))

             ; default serialization of a keyword drops the namespace
             (map (fn [id]
                    (subs (str id) 1))))})

(defn- do-create-session [auth interested-ids]
  (POST (str push-url-base "/sessions")
        (session-args auth interested-ids)))

(defn create-session [interested-ids]
  (when-let [auth (providers/watch-auth-map interested-ids)]
    (go (when-let [[err {:keys [id]}] (<! (do-create-session auth interested-ids))]
          (if err
            (log/warn "Unable to create push session" err)
            (>evt [::session-created interested-ids id]))))))

(defn connect [session-id]
  (log "Connecting to session " session-id)
  (doto (js/EventSource.
          (str push-url-base "/sessions/" session-id))
    (.addEventListener "error" (fn [e]
                                 (log/warn "Error with push session" e)))
    (.addEventListener "open" (fn []
                                (log/info "Connected to session " session-id)))
    (.addEventListener "message" (fn [evt]
                                   (log/info "Pushed: " evt (.-data evt))))))
