(ns wish.views.widgets.drag-drop
  (:require [clojure.string :as str]
            [reagent.core :as r]
            ["react-beautiful-dnd" :refer [DragDropContext Droppable Draggable]]))


(declare draggable)

; ======= util ============================================

(defn- unpack-opts [{:keys [id attrs] :as opts}]
  (let [attrs (when (fn? attrs)
                attrs)
        container-opts (select-keys opts [:type])
        opts (dissoc opts :id :attrs :type)]
    [(str id) container-opts opts attrs]))

(defn- flatten-children [children]
  (mapcat
    (fn [c]
      (if (and (coll? c)
               (vector? (first c)))
        c
        [c]))
    children))

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

(defn- unpack-droppable-id [id]
  (let [[nsp n part] (-> id (subs 1) (str/split #"/"))]
    [(keyword nsp n) (keyword part)]))

(defn- unpack-drag-end [ev]
  (when (and (.-source ev)
             (.-destination ev))
    (let [source (.-source ev)
          dest (.-destination ev)
          source-index (.-index source)
          dest-index (.-index dest)
          source-id (.-droppableId source)
          dest-id (.-droppableId dest)]
      (when (and (= "DROP" (.-reason ev))
                 (not (and (= source-index dest-index)
                           (= source-id dest-id))))
        {:from (conj (unpack-droppable-id source-id) source-index)
         :to (conj (unpack-droppable-id dest-id) dest-index)
         :item (let [[nsp n] (-> (.-draggableId ev) (subs 1) (str/split #"/"))]
                 (keyword nsp n))}))))

(defn drag-drop-context [opts & children]
  (let [on-drag-end (when-let [handler (:on-drag-end opts)]
                      (fn on-drag-end [ev]
                        (when-let [unpacked (unpack-drag-end ev)]
                          (handler unpacked))))
        opts (assoc opts :on-drag-end on-drag-end)]
    (into [:> DragDropContext opts]
          children)))

(defn droppable
  "Droppable is an abstraction over a :div that can accept elements being
   dropped into it. Its usage is equivalent to `[:div opts ...children]`.

   `opts` MUST contain `:id`, which must be unique across the parent
   drag-drop-context.

   `opts` MAY container `:type`, used as the equivalent property on
   the `Droppable`.

   If `opts` contains a fn `:attrs`, it is assumed to be a Spade style
   fn, which will get applied to the `:div`. The `:attrs` fn will be
   called with a boolean value of `true` when an item is being dragged
   over it, and `false` otherwise.

   Every other key of `opts` will be merged into the `:div`'s map."
  [opts & children]
  (let [[id container-opts opts attrs] (unpack-opts opts)]
    [:> Droppable (assoc container-opts
                         :droppable-id id)
     (fn [provided snapshot]
       (let [combined-opts (merge opts
                                  (when attrs
                                    (attrs (.-isDraggingOver snapshot)))
                                  {:ref (.-innerRef provided)}
                                  (js->clj (.-droppableProps provided)))]
         (r/as-element
           (conj
             (->> children
                  flatten-children
                  indexify-children
                  (into [:div combined-opts]))
             (.-placeholder provided)))))]))

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
   - `:attrs`, as per `droppable`
   - `:type`, as per `droppable`"
  [{:keys [index] :as opts} & children]
  (let [[id container-opts opts attrs] (unpack-opts (dissoc opts :index))]
    [:> Draggable (assoc container-opts
                         :draggable-id (str id)
                         :index index)
     (fn [provided snapshot]
       (let [combined-opts (merge opts
                                  (when attrs
                                    (attrs (.-isDragging snapshot)))
                                  {:ref (.-innerRef provided)}
                                  (js->clj (.-draggableProps provided))
                                  (js->clj (.-dragHandleProps provided)))]
         (r/as-element
           (into
             [:div combined-opts]
             children))))]))
