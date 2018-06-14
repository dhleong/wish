(ns ^{:author "Daniel Leong"
      :doc "fx"}
  wish.fx
  (:require [re-frame.core :refer [reg-fx]]
            [wish.providers :as providers :refer [load-sheet!]]
            ))

(reg-fx :providers/init! providers/init!)
(reg-fx :load-sheet! load-sheet!)
