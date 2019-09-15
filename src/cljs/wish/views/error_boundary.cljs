(ns ^{:author "Daniel Leong"
      :doc "error-boundary"}
  wish.views.error-boundary
  (:require [reagent.core :as r]))

(defn error-boundary [& _]
  (r/with-let [err (r/atom nil)]
    (r/create-class
     {:display-name "Error Boundary"

      :component-did-catch (fn [_this error #_info]
                             ;; (js/console.warn error info)
                             (reset! err error))

      :reagent-render (fn [& children]
                         (if-let [e @err]
                           [:div.error
                            [:h1 "Oops! Something went wrong"]
                            [:pre (if (ex-message e)
                                    (.-stack e)
                                    (str e))]]

                           (into [:<>] children)))})))
