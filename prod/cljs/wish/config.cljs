(ns ^{:author "Daniel Leong"
      :doc "Prod config"}
  wish.config)

(goog-define ^string VERSION "PROD-SNAPSHOT")

(def debug? false)

; IE: https://dhleong.github.io/wish
(def server-root "/wish")

(def gdrive-client-id "661182319990-3aa8akj9fh8eva9lf7bt02621q2i18s6.apps.googleusercontent.com")

(def push-server "https://wish-server.now.sh")
