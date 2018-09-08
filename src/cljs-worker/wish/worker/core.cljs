(ns wish.worker.core
  (:require-macros [wish.util.log :as log :refer [log]])
  (:require [clojure.string :as str]
            [cemerick.url :as url]))

(def cache-name "wish")

(def files-to-cache #js ["/"
                         "/js/compiled/app.js"
                         "/css/site.css"

                         ; external resources:
                         ;; "https://apis.google.com/js/api.js"
                         "https://fonts.googleapis.com/icon?family=Material+Icons"])


; ======= cache manipulation ==============================

(defn purge-old-caches []
  (-> js/caches
      .keys
      (.then #(->> %
                   (keep (fn [cache-key]
                           (when-not (= cache-name cache-key)
                             (.delete js/caches cache-key))))
                   clj->js
                   js/Promise.all))
      (.then #(log "Purged old cache entries"))))


; ======= resource fetching ===============================

(defn fetch-and-cache [req]
  (-> (js/fetch req)
      (.then (fn [resp]
               (log "Attempt to cache: " req)
               (let [resp-clone (.clone resp)]
                 (-> js/caches
                     (.open cache-name)
                     (.then #(.put % req resp-clone))))
               resp))))

(defn fetch-with-cache [to-match ev]
  ; we always prefer network if we can
  (-> (fetch-and-cache to-match)
      (.catch (fn [e]
                (log "Unable to fetch " to-match "; checking cache. e=" e)
                (-> js/caches
                    (.match to-match)
                    (.then (fn [resp]
                             (if resp
                               resp
                               (do
                                 (log/warn "Couldn't fetch " to-match " from cache")
                                 (throw e))))))))))

(defn never-cache? [url]
  ; don't cache other schemes
  (not (contains? #{"http" "https"}
                  (:protocol url))))

(defn shell? [url]
  (and
    (contains? #{"localhost"
                 "dhleong.github.io"}
               (:host url))
    ; all shell URLs have an anchor locally
    (or (not (str/blank? (:anchor url)))

        ; files with these extensions are never the shell
        (let [path (:path url)]
          (not (or (str/ends-with? path ".js")
                   (str/ends-with? path ".css")
                   (str/ends-with? path ".json")
                   (str/ends-with? path ".edn")))))))

(defn fetch-shell [ev]
  ; the shell is unlikely to change
  (log "fetching shell for " ev)
  (-> js/caches
      (.match "/")
      (.then (fn [resp]
               (or resp
                   (fetch-and-cache "/"))))))


; ======= event handlers ==================================

(defn fetch-event [ev]
  (let [request (.-request ev)
        url (-> request .-url url/url)]
    (cond
      (shell? url)
      (fetch-shell ev)

      (never-cache? url)
      (do
        (log "Never cache: " url)
        (js/fetch request))

      :else
      (fetch-with-cache request ev))))


(defn install-service-worker [ev]
  (log "Installing" ev)
  (-> js/caches
      (.open cache-name)
      (.then (fn [cache]
               (log "Caching app shell...")
               (.addAll cache files-to-cache)))
      (.then #(log "Successfully installed!")))
)

(defn activate-service-worker [ev]
  (log "Activated.")
  (purge-old-caches))


; ======= Attach events ===================================

(.addEventListener js/self "install" #(.waitUntil % (install-service-worker %)))
(.addEventListener js/self "fetch" #(.respondWith % (fetch-event %)))
(.addEventListener js/self "activate" #(.waitUntil % (activate-service-worker %)))
