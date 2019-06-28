(ns ^{:author "Daniel Leong"
      :doc "Virtual list"}
  wish.views.widgets.virtual-list
  (:require [reagent.core :as r]
            [cljsjs.react-virtualized]))

(def ^:private virtualized-list (r/adapt-react-class js/ReactVirtualized.List))
(def ^:private auto-sizer (r/adapt-react-class js/ReactVirtualized.AutoSizer))
(def ^:private cell-measurer (r/adapt-react-class js/ReactVirtualized.CellMeasurer))

(defn virtual-list
  "virtual-list is perhaps not the most optimized implementation, but it does
   provide a simple way to convert a lazy list to a virtualized one. Simply
   provide the :items to render, and a :render-item fn, and we will do the rest.

   This element must be wrapped in a :div with a fixed height.

   :items is the list (sequential collection) of items to render
   :render-item must accept a single argument with the item to render
   :overscan-row-count (optional, default 10) is an Int indicating how many
                       extra rows to render above and below the visible area
                       for smoother scrolls"
  [& {:keys [items render-item overscan-row-count]
      :or {overscan-row-count 10}}]
  {:pre [(identity render-item)
         (identity items)]}

  ; DON'T persist the cache, else changes to the items
  ; (for example, marking some "selected" or not) will NOT allow them
  ; to render with a different height
  (let [cache (js/ReactVirtualized.CellMeasurerCache.
                #js {:fixedWidth true})]
    [auto-sizer
     (fn [js-size]
       (r/as-element
         [virtualized-list
          {:height (aget js-size "height")
           :width (aget js-size "width")
           :deferred-measurement-cache cache
           :overscan-row-count overscan-row-count
           :row-count (count items)
           :row-height (.-rowHeight cache)
           :row-renderer (fn [row]
                           (let [{:keys [index key parent style]} (js->clj row :keywordize-keys true)]
                             (r/as-element
                               [cell-measurer
                                {:cache cache
                                 :column-index 0
                                 :key key
                                 :parent parent
                                 :row-index index}
                                [:div.row-entry {:style style}
                                 (render-item
                                   (nth items index))]])))}
          ]))]))
