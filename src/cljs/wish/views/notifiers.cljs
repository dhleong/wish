(ns wish.views.notifiers
  (:require [archetype.views.error-boundary :refer [error-boundary]]
            [spade.core :refer [defattrs defkeyframes]]
            [wish.style.media :as media]
            [wish.util :refer [<sub click>evts]]
            [wish.events :as events]
            [wish.views.widgets :refer [icon link>evt]]))

(defattrs notifiers-attrs []
  (at-media media/smartphones
    {:left 0
     :right 0
     :bottom 0})

  {:position 'fixed
   :right "24px"
   :bottom "24px"
   :z-index 2})

(defkeyframes slide-in []
  [:from {:transform "translateY(100%)"}])

(defattrs notifier-attrs []
  {:display 'flex
   :flex-direction 'row
   :align-items 'center

   ; https://material.io/design/motion/speed.html#easing
   :animation [[(slide-in) "250ms" "cubic-bezier(0, 0, 0.2, 1)"]]

   :background-color "#666666"
   :color "#fff"

   :padding "4px"
   :vertical-align 'center
   :margin-top "8px"}

  [:.ignore {:justify-content 'center}
   [:.link {:display 'flex
            :padding "8px"
            :vertical-align 'middle}]]

  [:.content {:padding "8px"
              :flex-grow 1}]
  [:.action {:margin-right "8px"
             :padding "8px"}])


; ======= public interface ================================

(defn notifier
  [{:keys [id
           dismiss-event
           content
           action-label
           action-event
           action-dismiss?]
    :or {action-dismiss? true}}]
  [:div (notifier-attrs)
   (when dismiss-event
     [:div.ignore
      [link>evt {:class "link"
                 :> dismiss-event}
       (icon :close)]])

   [:div.content
    [error-boundary
     content]]

   (when action-event
     [:div.action
      [:a {:href "#"
           :on-click (click>evts
                       action-event
                       (when action-dismiss?
                         [::events/remove-notify! id]))}
       action-label]])])

(defn update-notifier []
  (when (<sub [:update-available?])
    [notifier
     {:dismiss-event [:ignore-latest-update]
      :content "New version of WISH available!"
      :action-label "Update"
      :action-event [:update-app]}]))

(defn notifiers []
  [:div (notifiers-attrs)
   (when-let [notifications (seq (<sub [:notifications]))]
     (for [{:keys [id] :as n} notifications]
       ^{:key id}
       [notifier n]))

   [update-notifier]])
