(ns wish.sheets.dnd5e.subs.proficiency
  (:require [clojure.string :as str]
            [re-frame.core :as rf :refer [reg-sub]]
            [wish.sheets.dnd5e.data :as data]
            [wish.sheets.dnd5e.subs.inventory :as inventory]
            [wish.sheets.dnd5e.subs.util
             :refer [feature-by-id feature-in-lists]]))


; ======= const ===========================================

(def ^:private static-resistances
  #{:acid :cold :fire :lightning :poison})


; ======= subs ============================================

(defn level->proficiency-bonus
  [level]
  (condp <= level
    17 6
    13 5
    9 4
    5 3
    ; else
    2))

(reg-sub
  ::bonus
  :<- [:total-level]
  (fn [total-level _]
    (level->proficiency-bonus total-level)))

; returns a set of ability ids
(reg-sub
  ::saves
  :<- [:classes]
  (fn [classes _]
    (->> classes
         (filter :primary?)
         (mapcat :attrs)
         (filter (fn [[k v]]
                   (when (= v true)
                     (= "save-proficiency" (namespace k)))))
         (map (comp keyword name first))
         (into #{}))))

; returns a collection of feature ids
(reg-sub
  ::all
  :<- [:races]
  :<- [:classes]
  (fn [entity-lists _]
    (->> entity-lists
         flatten
         (mapcat (fn [{:keys [attrs]}]
                   (concat (:skill-proficiencies attrs)
                           (:proficiency attrs))))
         ; we now have a seq of proficiency -> true/false: clean up
         (keep (fn [[k v]]
                 (when v k)))
         (into #{}))))

(reg-sub
  ::others
  :<- [:sheet-engine-state]
  :<- [::all]
  (fn [[data-source feature-ids] _]
    (->> feature-ids
         (remove data/skill-feature-ids)
         (keep (partial feature-by-id data-source))
         (sort-by :name))))

; returns a collection of features
(reg-sub
  ::ability-extras
  :<- [:sheet-engine-state]
  :<- [:races]
  :<- [:classes]
  :<- [::inventory/attuned]
  :<- [:effects]
  (fn [[data-source & entity-lists] _]
    (->> entity-lists
         flatten
         (mapcat (comp
                   (partial apply concat)
                   (juxt (comp :saves :attrs)
                         (comp :immunities :attrs)
                         (comp :resistances :attrs))))

         ; TODO include the source?
         (map (fn [[id extra]]
                (cond
                  (true? extra)
                  (if (contains? static-resistances id)
                    ; static
                    {:id id
                     :desc (str "You are resistant to "
                                (str/capitalize (name id))
                                " damage.")}

                    ; not static? okay, it could be a feature
                    (if-let [f (feature-in-lists data-source entity-lists id)]
                      ; got it!
                      f

                      ; okay... effect?
                      (if-let [e (get-in data-source [:effects id])]
                        {:id id
                         :desc [:<>
                                (:name e) ":"
                                [:ul
                                 (for [line (:effects e)]
                                   ^{:key line}
                                   [:li line])]]}

                        ; halp
                        {:id id
                         :desc (str "Unknown: " id " / " extra)})))

                  ; full feature
                  (:id extra)
                  extra

                  ; shorthand (eg: just {:desc}):
                  :else
                  (assoc extra :id id)))))))

; returns a collection of feature ids
(reg-sub
  ::languages
  :<- [:sheet-engine-state]
  :<- [:all-attrs]
  (fn [[data-source all-attrs] _]
    (->> all-attrs
         :languages
         keys
         (keep (partial feature-by-id data-source))
         (sort-by :name))))


