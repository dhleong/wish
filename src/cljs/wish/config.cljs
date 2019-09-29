(ns wish.config)

(goog-define ^string VERSION "DEV-SNAPSHOT")

(def debug?
  ^boolean goog.DEBUG)

(goog-define server-root "")
(goog-define full-url-root "http://localhost:3450")

(goog-define gdrive-client-id "661182319990-1uerkr0pue6k60a83atj2f58md95fb1b.apps.googleusercontent.com")

(goog-define push-server "http://localhost:4321")
