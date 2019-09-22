(ns wish.sheets.dnd5e.engine
  (:require [wish-engine.core :as engine]
            [wish.sheets.dnd5e.util :as util]))

(def ^:private hooks
  {:inflate-class util/post-process
   :inflate-race util/post-process})

(defn create-engine []
  (engine/create-engine {:hooks hooks}))
