(ns wish.views.widgets.drag-drop
  (:require [reagent.core :as r]
            ["react-beautiful-dnd" :refer [DragDropContext Droppable Draggable]]))


(declare draggable)

; ======= util ============================================

(defn- unpack-opts [{:keys [id attrs] :as opts}]
  (let [attrs (when (fn? attrs)
                attrs)
        opts (dissoc opts :id :attrs)]
    [(str id) opts attrs]))

(defn- indexify-children [children]
  (let [last-index (volatile! -1)]
    (for [c children]
      (let [draggable-form? (and (vector? c)
                                 (identical? draggable (first c))
                                 (map? (second c)))]
        (if (and draggable-form?
                 (not (:index (second c))))
          ; common case: index not provided; generate the
          ; index
          (assoc-in c [1 :index] (vswap! last-index inc))

          ; other case: either not a draggable (and thus doesn't
          ; need an index) or index wasprovided
          c)))))

; ======= public interface ================================

(defn drag-drop-context [opts & children]
  (into [:> DragDropContext opts]
        children))

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
  (let [[id opts attrs] (unpack-opts opts)]
    [:> Droppable {:droppable-id id}
     (fn [provided snapshot]
       (r/as-element
         (into
           [:div (merge opts
                        (when attrs
                          (attrs (.-isDraggingOver snapshot)))
                        {:ref (.-innerRef provided)}
                        (js->clj (.-droppableProps provided)))
            (.-placeholder provided)]

           (indexify-children children))))]))

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
  [{:keys [index] :as opts} & children]
  (let [[id opts attrs] (unpack-opts opts)]
    [:> Draggable {:draggable-id (str id)
                   :index index}
     (fn [provided snapshot]
       (r/as-element
         (into
           [:div (merge opts
                        (when attrs
                          (attrs (.-isDragging snapshot)))
                        {:ref (.-innerRef provided)}
                        (js->clj (.-draggableProps provided))
                        (js->clj (.-dragHandleProps provided)))]
           children)))]))
