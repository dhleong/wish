(ns ^{:author "Daniel Leong"
      :doc "Campaign workspace"}
  wish.views.campaign.workspace
  (:require [spade.core :refer [defattrs]]
            [wish.views.widgets.drag-drop :refer [drag-drop-context draggable droppable]]
            [wish.util :refer [<sub]]
            [wish.subs.campaign.workspace :as workspace]))

; ======= styles ==========================================

(defattrs workspace-style []
  {:height "300px"}) ; TODO ??

(defattrs droppable-style [dropping-over?]
  {:height "70%"})

(defattrs primary-col-style [dropping-over?]
  {:composes (droppable-style dropping-over?)
   :width "60vw"})

(defattrs secondary-col-style [dropping-over?]
  {:composes (droppable-style dropping-over?)
   :width "25vw"})


; ======= util ============================================

(defn- id-for [item]
  (or (when (keyword? item)
        item)
      (:id item)))


; ======= space ===========================================

(defn- space [entity-card item]
  (println "SPACE=" item)
  (let [{primary :p secondary :s} item]
    ; TODO
    [:div.space
     [droppable {:id (str (:id item) "/primary")
                 :type "all"
                 :attrs primary-col-style}
      [draggable {:id (:id primary)}
       [entity-card primary]]]

     [droppable {:id (str (:id item) "/secondary")
                 :type "all"
                 :attrs secondary-col-style}
      (for [s secondary]
        ^{:key (:id s)}
        [draggable {:id (:id s)
                    :class "draggable"}
         [entity-card s]])]]))

; ======= public interface ================================

(defn- on-drag-end [result]
  (println "END: " result))

(defn workspace [& {:keys [entity-card]}]
  (let [spaces (<sub [::workspace/spaces])]
    [:div (workspace-style)
     [drag-drop-context {:on-drag-end on-drag-end
                         :on-drag-start #(println "START " %)
                         :on-drag-update #(println "UPDATE " %)
                         }
      (for [s spaces]
        ^{:key (id-for s)}
        [space entity-card s])]]))
