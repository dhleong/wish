(ns wish.sheets.dnd5e.overlays.starter-eq
  (:require-macros [wish.util :refer [fn-click]]
                   [wish.util.log :as log :refer [log]])
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [wish.sheets.dnd5e.subs.starter :as starter]
            [wish.sheets.dnd5e.style :as styles]
            [wish.util :refer [<sub >evt toggle-in]]
            [wish.views.widgets.fast-numeric]))

(defn- direct-click?
  "Return true if the click event was directly on the
   desired element"
  [e]
  ; okay, sort of; this is slightly simpler than having
  ; to track the actual dom elements
  (not (#{"SELECT"}
         (.. e -target -tagName))))

(defn- equipment-choice
  [state path choices enabled?]
  [:select
   {:on-change (fn-click [e]
                 (swap! state
                        assoc-in path
                        (int (.. e -target -value))))
    :value (if-let [v (get-in @state path)]
             v
             js/undefined)
    :disabled (not enabled?)}
   (for [[i c] (map-indexed list choices)]
     ^{:key (:id c)}
     [:option {:value i}
      (:name c)])])

(defn- equipment-count
  [item amount]
  [:span [:b "(" amount ") "] (:name item)])

(defn- equipment-pack
  ([pack]
   (equipment-pack pack false))
  ([pack expanded?]
   [:div.pack
    [:div.name (:name pack)]
    (when expanded?
      [:div.contents
       (->> pack
            :contents
            (map (fn [[item amount]]
                   (str amount " " (:name item))))
            (str/join ", "))])]))

(defn- equipment-and
  [state path values chosen?]
  (let [top-level? (= 1 (count path))
        chosen-path (conj path :chosen)
        options-count (count values)]
    [:div
     (when top-level?
       {:class "alternatives clickable"
        :on-click (fn-click [e]
                    (when (direct-click? e)
                      (swap! state toggle-in chosen-path true)))})
     [:div
      {:class (when top-level?
                ["choice" (when chosen?
                            "chosen")])}
      (for [[i v] (map-indexed list values)]
        ^{:key i}
        [:span
         (cond
           (= i 0) nil
           (= i (dec options-count)) " and "
           :else ", ")
         (cond
           (and (vector? v)
                (= :or (first v)))
           [equipment-choice state (conj path i) (second v) chosen?]

           (and (vector? v)
                (= :count (first v)))
           [equipment-count (second v) (peek v)]

           ; single item
           :else
           (:name v))])]]))

(defn- equipment-or
  [state path choices]
  (let [chosen-path (conj path :chosen)
        chosen (get-in @state chosen-path)]
    [:div.alternatives
    (for [[i v] (map-indexed list choices)]
      (let [chosen? (= chosen i)]
        ^{:key i}
        [:div.choice.clickable
         {:on-click (fn-click [e]
                      ; ignore clicks on contained, clickable children (esp [select])
                      (when (direct-click? e)
                        (swap! state toggle-in chosen-path i)))
          :class (when chosen?
                   "chosen")}
         (if (vector? v)
           (let [[kind v amount] v]
             ; special case
             (case kind
               :count [equipment-count v amount]
               :pack [equipment-pack v (when chosen?
                                         :expand!)]
               :and [equipment-and state (conj path i) v chosen?]
               :or (if chosen?
                     [equipment-choice state (conj path i) v :enabled]
                     [:span "(choice)"])))

           ; single item
           [:span (:name v)])]))]))

(defn expand-starting-eq
  ([choices state-map]
   (->> choices
        (map-indexed list)
        (mapcat
          (fn [[i outer-choice]]
            (expand-starting-eq outer-choice state-map [i] false)))))

  ([choice state-map path and?]
   (when-let [chosen (or (get-in state-map (conj path :chosen))
                         (get-in state-map path)
                         and?)]
     (if (vector? choice)
       (let [[kind values ?amount] choice]
         (case kind
           ; wacky (when-not) to handle de-selected top-level and
           :and (when-not (and (map? chosen)
                               (contains? chosen :chosen)
                               (not (:chosen chosen)))
                  (->> values
                       (map-indexed list)
                       (mapcat
                         (fn [[i item]]
                           (expand-starting-eq item state-map
                                               (conj path i)
                                               :and!)))))

           :or (when-let [chosen (if (number? chosen)
                                   chosen

                                   ; top-level :or do NOT have a default
                                   ; value, but nested ones do
                                   (when (> (count path) 1)
                                     0))]
                 (expand-starting-eq
                   (nth values chosen)
                   state-map
                   (conj path chosen)
                   true))

           ; easy peasy
           :count [[values ?amount]]

           ; also easy
           :pack (-> values :contents)))

       ; simple case
       [choice]))))


; ======= public interface ================================

(defn overlay []
  (r/with-let [state (r/atom {})]
    (let [{primary-class :class
           choices :choices} (<sub [::starter/eq])
          this-state @state]
      [:div styles/starting-equipment-overlay
       [:h5 (:name primary-class) " Starting Equipment"]

       (for [[i [kind values]] (map-indexed list choices)]
         (with-meta
           (case kind
             :or [equipment-or state [i] values]
             :and [equipment-and state [i] values
                   (when (get this-state i)
                     :chosen!)])
           {:key i}))

       (when (some :chosen (vals this-state))
         [:div.accept
          [:a {:href "#"
               :on-click (fn-click
                           (let [items (expand-starting-eq
                                         choices
                                         @state)]
                             (log "State:" @state)
                             (log "Add items: " items)
                             (>evt [:inventory-add-n items])
                             (>evt [:toggle-overlay nil])))}
           "I'll take it!"]])])))
