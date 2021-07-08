(ns ^{:author "Daniel Leong"
      :doc "footer"}
  wish.views.footer
  (:require [garden.color :as color]
            [spade.core :refer [defattrs]]
            [wish.config :refer [server-root VERSION]]
            [wish.style :as style]
            [wish.style.flex :as flex]
            [wish.style.media :as media]
            [wish.util :refer [>evt]]))

(defattrs footer-container-attrs []
  (merge flex/vertical
         {:height "100vh"})
  [:&>.content {:flex [[1 0 :auto]]
                :padding "16px"}])

(def ^:private link-default "#333")
(def ^:private link-dark style/header-primary-on-dark)

(defattrs footer-attrs []
  (merge flex/flex
         {:flex-shrink 0
          :background-color "#eee"
          :font-size :80%
          :margin-top "12px"
          :padding "16px"})
  [:a.link {:color link-default}
   ["&:not(:first-child):before" {:content "'·'"
                                  :display :inline-block
                                  :text-align :center
                                  :width "1em"}]
   [:&:hover {:color "#999"}
    ["&:not(:first-child):before" {:color link-default}]]]
  [:.version (merge flex/grow
                    {:text-align :right
                     :color (color/transparentize link-default 0.93)
                     :cursor :pointer})]

  (at-media media/dark-scheme
    {:background-color "#333"}
    [:a.link {:color link-dark}
     ["&:hover:not(:first-child):before" {:color link-dark}]]
    [:.version {:color (color/transparentize link-dark 0.93)}]))

(defn footer []
  [:footer (footer-attrs)
   [:a.link {:href (str server-root "/privacy.html")
             :target '_blank}
    "Privacy Policy"]

   [:a.link {:href "https://github.com/dhleong/wish"
             :target '_blank}
    "Contribute"]

   [:a.link {:href "https://github.com/dhleong/wish/issues"
             :target '_blank}
    "Issues"]

   [:div.version {:on-click (fn [e]
                              (.stopPropagation e)
                              (>evt [:update-app]))}
    VERSION]
   ])

