(ns ^{:author "Daniel Leong"
      :doc "Campaign workspace"}
  wish.views.campaign.workspace
  (:require [spade.core :refer [defattrs]]
            [wish.views.widgets.drag-drop :refer [drag-drop-context draggable droppable]]
            [wish.util :refer [>evt <sub]]
            [wish.subs.campaign.workspace :as workspace]
            [wish.events.campaign.workspace :as workspace-events]
            [wish.style.flex :as flex]
            [wish.views.widgets :refer [formatted-text]]))

; ======= styles ==========================================

(defattrs workspace-style []
  (flex/create
    :flow :vertical
    :center :horizontal

    {:width "100vw"}))

(defattrs space-style []
  (flex/create
    {:height "250px"}))

(defattrs droppable-style [dropping-over?]
  {:background (if dropping-over?
                 "#aee"
                 "#eee")})

(defattrs primary-col-style []
  {:composes (droppable-style false)
   :width "50vw"})

(defattrs secondary-col-style [dropping-over?]
  {:composes (droppable-style dropping-over?)
   :background (when-not dropping-over?
                 :background "#fafafa")
   :overflow-y 'auto
   :width "35vw"})

(defattrs entity-draggable-style [dragging?]
  (when dragging?
    {:background "#ccc !important"
     :border-radius "8px"
     :box-shadow "0 8px 6px -6px black"
     :overflow 'hidden}))


; ======= built-in card types =============================

(defattrs note-style []
  {:padding "12px"})

(defn- note-card [entity]
  [:div (note-style)
   (when-let [n (:n entity)]
     [:h3 n])

   [formatted-text :div.contents (:contents entity)]])


; ======= space ===========================================

(defn- draggable-content [entity-card {:keys [kind] :as entity}]
  (case kind
    ; NOTE: built-in entity card types for eg notes
    :note [note-card entity]

    ; fallback to a custom entity card for the sheet:
    [entity-card entity]))

(defn- entity-draggable
  "NOTE: this should be called as a function so that the
   `draggable` may be a top-level child of the `droppable`"
  [entity-card {:keys [id] :as entity}]
  ^{:key id}
  [draggable {:id id
              :attrs entity-draggable-style}
   [draggable-content entity-card entity]])

(defn- space [i entity-card item]
  (let [{:keys [id primary secondary]} item]
    [draggable {:id id
                :index i
                :attrs space-style}
     [:div (primary-col-style)
      ; the primary of a story cannot be dragged
      [draggable-content entity-card primary]]

     [droppable {:id (str (:id item) "/secondary")
                 :type :notes
                 :attrs secondary-col-style}
      (for [s secondary]
        (entity-draggable entity-card s))]]))


; ======= public interface ================================

(defn- on-drag-end [{:keys [item from to] :as ev}]
  (println "DROP " item "FROM " from " ONTO " to)
  (>evt [::workspace-events/drag ev]))

(defn workspace [& {:keys [entity-card]}]
  (let [spaces (<sub [::workspace/spaces])]
    [:div (workspace-style)
     [drag-drop-context {:on-drag-end on-drag-end}
      [droppable {:id :spaces
                  :type :spaces}
       (for [[i s] (map-indexed list spaces)]
         ^{:key (:id s)}
         [space i entity-card s])]]]))
