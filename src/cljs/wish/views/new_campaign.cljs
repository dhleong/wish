(ns ^{:author "Daniel Leong"
      :doc "new-campaign"}
  wish.views.new-campaign
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [wish.util :refer [fn-click]]
                   [wish.util.log :as log])
  (:require [clojure.core.async :refer [chan <!]]
            [clojure.string :as str]
            [reagent.core :as r]
            [wish.providers :as providers]
            [wish.sheets :as sheets]
            [wish.util :refer [<sub]]
            [wish.util.nav :as nav :refer [sheet-url]]
            [wish.views.widgets :refer [icon link]]))

(defn page []
  [:div
   [:h3
    [link {:href "/"}
     (icon :home)]
    "New Campaign"]])
