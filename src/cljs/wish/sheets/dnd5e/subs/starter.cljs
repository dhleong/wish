(ns wish.sheets.dnd5e.subs.starter
  "Subs related to starter equipment packs"
  (:require-macros [wish.util.log :as log])
  (:require [re-frame.core :as rf :refer [reg-sub]]
            [wish-engine.core :as engine]
            [wish.util :refer [->map]]))

(reg-sub
  ::packs-by-id
  :<- [:sheet-engine-state]
  (fn [source]
    (->map
      (engine/inflate-list source :5e/starter-packs))))

(defn- select-filter-keys
  "Like (select-keys) but any keys that weren't missing
   get a default value of false"
  [item keyseq]
  (reduce
    (fn [m k]
      (assoc m k (get item k false)))
    {}
    keyseq))

(defmulti unpack-eq-choices (fn [_source _packs choices]
                              (cond
                                (vector? choices) :and
                                (list? choices) :or
                                (map? choices) :filter
                                (keyword? choices) :id
                                :else
                                (do
                                  (log/warn "Unexpected choices: " choices)
                                  (type choices)))))
(defmethod unpack-eq-choices :or
  [source packs choices]
  [:or (map (partial unpack-eq-choices source packs) choices)])
(defmethod unpack-eq-choices :and
  [source packs choices]
  [:and (map (partial unpack-eq-choices source packs) choices)])
(defmethod unpack-eq-choices :filter
  [source _ choices]
  (if-let [id (:id choices)]
    ; single item with a :count
    [:count (get-in source [:items id]) (:count choices)]

    ; filter
    (let [choice-keys (keys choices)]
      [:or (->> source :items vals  ; all items
                (remove :+) ; no magic items
                (remove :desc) ; or fancy items
                (filter (fn [item]
                          ; would it be more efficient to just make a
                          ; custom = fn here?
                          (let [matching-keys (select-filter-keys item choice-keys)]
                            (= matching-keys choices)))))])))
(defmethod unpack-eq-choices :id
  [source packs choice]
  (or (when-let [p (get packs choice)]
        ; packs are special
        [:pack (update p :contents
                       (partial
                         map
                         (fn [[id amount]]
                           [(get-in source [:items id])
                            amount])))])

      ; just an item
      (get-in source [:items choice])))

(reg-sub
  ::eq
  :<- [:sheet-engine-state]
  :<- [::packs-by-id]
  :<- [:primary-class]
  (fn [[source packs {{eq :5e/starting-eq} :attrs
                      :as primary-class}]]
    {:class primary-class
     :choices
     (map
       (partial unpack-eq-choices source packs)
       eq)}))

