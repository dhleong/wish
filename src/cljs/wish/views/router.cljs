(ns ^{:author "Daniel Leong"
      :doc "router"}
  wish.views.router
  (:require [wish.util :refer [<sub >evt]]
            [wish.views.footer :refer [footer]]))

(defn- pick-page-title []
  (let [current-sheet-name (:name (<sub [:sheet-meta]))]
    (cond
      ; are we looking at a sheet?
      current-sheet-name (str current-sheet-name " [WISH]")

      ; default:
      :else "WISH")))

(defn- has-footer? [page]
  (not (contains? #{:campaign :sheet :splash}
                  page)))

(defn router
  "Renders the current page, given a map
   of page-id to page render fn."
  [routes-map]
  (let [[page args] (<sub [:page])
        page-form [(get routes-map page) args]]
    (>evt [:title! (pick-page-title)])
    (println "[router]" page args)

    (if (has-footer? page)
      ; we have to do a bit of wrapping to render the footer nicely
      [:div#footer-container
       [:div.content
        page-form]
       [footer]]

      ; no footer; just render the page directly
      page-form)))


