(ns wish.views.overlay
  (:require [garden.color :as color]
            [spade.core :refer [defclass]]
            [wish.style.flex :as flex]
            [wish.style.media :as media]
            [wish.style.shared :as shared]
            [wish.util :refer [<sub click>evt]]
            [wish.views.widgets :refer-macros [icon]]
            [wish.views.widgets.error-boundary :refer [error-boundary]]))

(defclass overlay-class []
  (merge flex/vertical-center
         flex/align-center
         {:position :fixed
          :top 0
          :left 0
          :right 0
          :bottom 0
          :height :100%
          :z-index 1

          :background-color (color/transparentize "#333" 0.2)})

  (at-media media/smartphones
    [:& {:justify-content :flex-start}]))

(defclass overlay-inner-base []
  {:position :relative
   :background "#f0f0f0"
   :border-radius "2px"
   :max-width "80%"}

  (at-media media/dark-scheme
    [:& {:background "#444"}])
  (at-media media/smartphones
    [:& {:width "85%"
         :max-width "85%"
         :height [[:100% :!important]]
         :max-height [[:100% :!important]]}

     ; Override padding and dimensions on mobile:
     [:.wrapper>:first-child {:padding "16px"
                              :width [[:100% :!important]]}]])

  [:.close-button (merge shared/clickable
                         {:position :absolute
                          :top "4px"
                          :right "4px"}) ]
  [:.wrapper (merge flex/flex
                    flex/justify-center
                    {:height :100%
                     :margin-top "8px"})])

(defclass overlay-inner []
  {:composes (overlay-inner-base)
   :max-height :80%
   :overflow-y :auto})

(defclass overlay-inner-scrollable []
  {:composes (overlay-inner-base)
   :height :80%}
  [:.scroll-host {:height :100%
                  :overflow-y :auto}])

(defn overlay []
  (when-let [[{:keys [scrollable?]} overlay-spec] (<sub [:showing-overlay])]
    [:div
     {:class (overlay-class)
      :on-click (click>evt [:toggle-overlay])}

     [:div
      {:class (if scrollable?
                (overlay-inner-scrollable)
                (overlay-inner))
       :on-click (fn [e]
                   ; prevent click propagation by default
                   ; to avoid the event leaking through and
                   ; triggering the dismiss click on the bg
                   (.stopPropagation e))}
      [:div.close-button
       {:on-click (click>evt [:toggle-overlay])}
       (icon :close)]

      ; finally, the overlay itself
      [:div.scroll-host
       [:div.wrapper
        [error-boundary
         overlay-spec]]]]]))
