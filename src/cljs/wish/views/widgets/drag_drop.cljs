(ns wish.views.widgets.drag-drop
  (:require [reagent.core :as r]
            ["react-beautiful-dnd" :refer [DragDropContext Droppable Draggable]]))

(defn drag-drop-context [opts & children]
  (into [:> DragDropContext opts]
        children))

(declare draggable)

(defn droppable
  "Droppable is an abstraction over a :div that can accept elements being
   dropped into it. Its usage is equivalent to `[:div opts ...children]`.

   `opts` MUST contain `:id`, which must be unique across the parent
   drag-drop-context.

   If `opts` contains a fn `:attrs`, it is assumed to be a Spade style
   fn, which will get applied to the `:div`. The `:attrs` fn will be
   called with a boolean value of `true` when an item is being dragged
   over it, and `false` otherwise.

   Every other key of `opts` will be merged into the `:div`'s map."
  [opts & children]
  (let [droppable-id (:id opts)
        attrs (when (fn? (:attrs opts))
                (:attrs opts))
        opts (dissoc opts :id :attrs)]
    [:> Droppable {:droppable-id (str droppable-id)}
     (fn [provided snapshot]
       (let [child-index (volatile! -1)]
         (r/as-element
           (into
             [:div (merge opts
                          (when attrs
                            (attrs (.-isDraggingOver snapshot)))
                          {:ref (.-innerRef provided)}
                          (js->clj (.-droppableProps provided)))
              (.-placeholder provided)]

             (for [c children]
               (if (and (vector? c)
                        (identical? draggable (first c))
                        (map? (second c))
                        (not (:index (second c))))
                 (assoc-in c [1 :index] (vswap! child-index inc))
                 c))))))]))

(defn draggable
  "Droppable is an abstraction over a :div that can be dragged. Its usage
   is equivalent to `[:div opts ...children]`.

   `opts` MUST contain:
   - `:id`, as per `droppable`

   `opts` SHOULD contain:
   - `:index`, a monotonically increasing integer indicating the index of
     this draggable within the parent `droppable`. If all your draggables
     are direct children of a `droppable` (they should be) you can omit
     this; otherwise, it is required.

   `opts` MAY contain:
   - `:attrs`, as per `droppable`"
  [opts & children]
  (let [draggable-id (:id opts)
        attrs (when (fn? (:attrs opts))
                (:attrs opts))
        index (:index opts)
        opts (dissoc opts :id :attrs)]
    [:> Draggable {:draggable-id (str draggable-id)
                   :index index}
     (fn [provided snapshot]
       (r/as-element
         (into
           [:div (merge opts
                        (when attrs
                          (attrs (.-isDragging snapshot)))
                        {:ref (.-innerRef provided)
                         :style (.. provided -draggableProps -style)}
                        (js->clj (.-draggableProps provided))
                        (js->clj (.-dragHandleProps provided)))]
           children)))]))
