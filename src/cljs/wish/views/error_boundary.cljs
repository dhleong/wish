(ns ^{:author "Daniel Leong"
      :doc "error-boundary"}
  wish.views.error-boundary
  (:require [reagent.core :as r]))

(when goog.DEBUG
  ; in debug builds, we can auto-retry rendering error'd components
  ; every time a load occurs
  (def ^:private active-err-atoms (atom #{}))

  (defn ^:dev/after-load clear-errors []
    (swap! active-err-atoms (fn [atoms]
                              (doseq [a atoms]
                                (reset! a nil))

                              ; clear
                              #{}))))
(defn error-boundary [& _]
  (r/with-let [err (r/atom nil)]
    (r/create-class
     {:display-name "Error Boundary"

      :component-did-catch (fn [_this error #_info]
                             ;; (js/console.warn error info)
                             (reset! err error))

       :statics
       #js {:getDerivedStateFromError (fn [error]
                                        (when goog.DEBUG
                                          ; enqueue the atom for auto-clearing
                                          (swap! active-err-atoms conj err))

                                        (reset! err error))}

      :reagent-render (fn [& children]
                         (if-let [e @err]
                           [:div.error
                            [:h1 "Oops! Something went wrong"]
                            [:pre (if (ex-message e)
                                    (.-stack e)
                                    (str e))]]

                           (into [:<>] children)))})))
