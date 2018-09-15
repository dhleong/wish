(ns ^{:author "Daniel Leong"
      :doc "footer"}
  wish.views.footer
  (:require [wish.config :refer [server-root VERSION]]))

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

   [:div.version
    VERSION]
   ])

