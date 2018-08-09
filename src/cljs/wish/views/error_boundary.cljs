(ns ^{:author "Daniel Leong"
      :doc "error-boundary"}
  wish.views.error-boundary
  (:require [clojure.string :as str]
            [reagent.core :as r]))

(defn error-boundary [& children]
  (r/with-let [err (r/atom nil)]
    (r/create-class
     {:display-name "Error Boundary"

      :component-did-catch (fn [this error info]
                             (js/console.warn error info)
                             (reset! err error))

      :reagent-render (fn [& children]
                        (if-let [err @err]
                          [:div.error
                           [:h1 "Oops! Something went wrong"]
                           [:div (str err)]]

                          (vec (cons :<> children))))})))
