(ns wish.worker.core
  (:require-macros [wish.util.log :as log :refer [log]])
  (:require [clojure.string :as str]
            [cemerick.url :as url]
            [wish.config :as config]))

(def cache-name "wish")

(def default-network-timeout-ms 15000)

(def shell-root (str config/server-root "/"))

(def files-to-cache #js [shell-root
                         (str config/server-root "/js/compiled/app.js")
                         (str config/server-root "/css/site.css")

                         ; external resources:
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

(defn fetch-with-timeout
  ([req]
   (fetch-with-timeout req default-network-timeout-ms))
  ([req timeout-ms]
   (js/Promise.
     (fn [presolve preject]
       (let [about-controller (js/AbortController.)
             timeout-timer (js/setTimeout
                             (fn []
                               (log/info "Abort slow request " req)
                               ; signal the request to stop
                               (.abort about-controller)
                               (preject (js/Error.
                                          "Request failed (network delay / timed out)")))
                             default-network-timeout-ms)]
         (-> (js/fetch req
                       #js {:signal (.-signal about-controller)})
             (.then presolve preject)
             (.finally #(js/clearTimeout timeout-timer))))))))

(defn fetch-and-cache [req]
  (-> (js/fetch req)

      (.then (fn [resp]
               (log "Attempt to cache: " req " -> " resp)
               (let [resp-clone (.clone resp)]
                 (-> js/caches
                     (.open cache-name)
                     (.then #(.put % req resp-clone))))
               resp))))

(defn fetch-with-cache [to-match]
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
  (or (not (contains? #{"http" "https"}
                      (:protocol url)))

      ; don't cache gapi; it's simpler to just use local-storage
      ; and handle caching sheets and data sources ourselves
      (contains? #{"apis.google.com"}
                 (:host url))))

(defn wish-asset? [url]
  (contains? #{"localhost"
               "dhleong.github.io"}
             (:host url)))

(defn asset-file? [url]
  (let [path (:path url)]
    (or (str/ends-with? path ".css")
        (str/ends-with? path ".edn")
        (str/ends-with? path ".js")
        (str/ends-with? path ".json")
        (str/ends-with? path ".png"))))

(defn shell-root? [url]
  (and (wish-asset? url)

       ; all shell URLs have an anchor locally
       (or (not (str/blank? (:anchor url)))

           ; files with these extensions are never the shell
           (not (asset-file? url)))))

(defn shell-asset? [url]
  (and (wish-asset? url)
       (asset-file? url)))

(defn fetch-shell [url]
  ; the shell is unlikely to change
  (log "fetching shell for " url)
  (-> js/caches
      (.match shell-root)
      (.then (fn [resp]
               (or resp
                   (fetch-and-cache shell-root))))))

(defn fetch-shell-asset [{:keys [path]}]
  ; the shell is unlikely to change
  (log "fetching shell asset for " path)
  (let [css-dir-start (.indexOf path "/css/")]
    (if (not= -1 css-dir-start)
      (fetch-with-cache (str config/server-root
                             (subs path css-dir-start)))

      (let [js-dir-start (.indexOf path "/js/")]
        (if (not= -1 js-dir-start)
          (fetch-with-cache (str config/server-root
                                 (subs path js-dir-start)))

          (fetch-with-cache path))))))


; ======= event handlers ==================================

(defn fetch-event [ev]
  (let [request (.-request ev)
        url (-> request .-url url/url)]
    (cond
      (shell-root? url)
      (fetch-shell url)

      (shell-asset? url)
      (fetch-shell-asset url)

      (never-cache? url)
      (do
        (log "Never cache: " url)
        (-> (js/fetch request)
            (.catch (fn [e]
                      (log/warn "Unable to fetch " url ": " e)
                      (js/Response. nil #js {:status 503})))))

      :else
      (fetch-with-cache request))))


(defn install-service-worker [ev]
  (log/info "Installing" ev)
  (-> js/caches
      (.open cache-name)
      (.then (fn [cache]
               (log "Caching app shell...")
               (.addAll cache files-to-cache)))
      (.then #(log "Successfully installed!")))
)

(defn activate-service-worker [ev]
  (log/info "Activated.")
  (purge-old-caches))


; ======= Attach events ===================================

(.addEventListener js/self "install" #(.waitUntil % (install-service-worker %)))
(.addEventListener js/self "fetch" #(.respondWith % (fetch-event %)))
(.addEventListener js/self "activate" #(.waitUntil % (activate-service-worker %)))
