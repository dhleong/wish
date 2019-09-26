(ns ^{:author "Daniel Leong"
      :doc "footer"}
  wish.views.footer
  (:require [wish.config :refer [server-root VERSION]]
            [wish.util :refer [>evt]]))

(defn footer []
  [:footer.footer
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

