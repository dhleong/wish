(ns ^{:author "Daniel Leong"
      :doc "errors"}
  wish.providers.gdrive.errors)

(defn resolve-permissions [id]
  [:div
   [:h4 "You may not have permission to view this file"]
   [:div "TODO"]])

(defn view
  [{:keys [id] :as data}]
  (cond
    (:permissions? data)
    [resolve-permissions id]))
