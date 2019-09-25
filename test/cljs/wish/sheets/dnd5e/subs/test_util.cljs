(ns wish.sheets.dnd5e.subs.test-util
  (:require [wish-engine.core :as engine]))

(defn ->ds
  [& directives]
  (let [eng (engine/create-engine)
        state (engine/create-state eng)]
    (doseq [d directives]
      (engine/load-source eng state d))
    @state))

