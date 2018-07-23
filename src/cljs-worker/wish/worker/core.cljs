(ns wish.worker.core)

(defn install-service-worker [e]
  (js/console.log "[ServiceWorker] Installing")
)

(defn activate-service-worker [e]
  (js/console.log "[ServiceWorker] Activated")
  )

#_(defn fetch-event [e]
  (js/console.log "[ServiceWorker] Fetch" (-> e .-request .-url))
  (let [request (.-request e)
        url (-> request .-url url/url)]
    (case (:host url)
      ("localhost" "pwa-clojure.staging.quintype.io")
      (fetch-page-or-cached (:path url) e)

      (fetch-cached request))))

(.addEventListener js/self "install" #(.waitUntil % (install-service-worker %)))
#_(.addEventListener js/self "fetch" #(.respondWith % (fetch-event %)))
(.addEventListener js/self "activate" #(.waitUntil % (activate-service-worker %)))
