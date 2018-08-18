(ns ^{:author "Daniel Leong"
      :doc "Fixed 5e SRD data to avoid too much dup
            in the data sources"}
  wish.sheets.dnd5e.data
  (:require [clojure.string :as str]))

; ======= armor and weapons ================================

(defn- ac<-dex+
  "Returns a fn that computes AC given dex plus
   the given `ac-base` value."
  ([ac-base]
   (ac<-dex+ ac-base 10)) ; basically, no limit on the modifier
  ([ac-base max-modifier]
   (fn [{{dex :dex} :modifiers}]
     (+ ac-base (min max-modifier
                     dex)))))

(defn- ->kind-maps
  "Given a map, takes the keys and converts them to a sorted
   collection of {:id,:label} maps, where the :label is
   derived from the id."
  [m]
  (->> m
       keys
       (map (fn [id]
              {:id id
               :label (-> id
                          name
                          (str/replace "-" " ")
                          str/capitalize)}))
       (sort-by :label)))

(def ^:private armor
  {
    ; light:
    :padded {:ac (ac<-dex+ 11)
             :disadvantage? :stealth}  ; 5 gp, 11 + Dex modifier — Disadvantage; 8 lb.
    :leather {:ac (ac<-dex+ 11)}  ; 10 gp, 11 + Dex; 10 lb
    :studded {:ac (ac<-dex+ 12)}  ; 45 gp, 11 + Dex; 13 lb

    ; med:
    :hide {:ac (ac<-dex+ 12 2)}  ; 10 gp, 12 + Dex (max 2); 12 lb
    :chain-shirt {:ac (ac<-dex+ 13 2)}  ; 50 gb, 13 + Dex (max 2); 20lb
    :scale-mail {:ac (ac<-dex+ 14 2)
                :disadvantage? :stealth}  ; 50 gp, 14 + Dex (max 2) — Disadvantage; 45 lb.
    :breastplate {:ac (ac<-dex+ 14 2)}  ; 400 gp, 14 + Dex (max 2); 20 lb
    :half-plate {:ac (ac<-dex+ 15 2)
                :disadvantage? :stealth}  ; 750 gp, 15 + Dex (max 2)— Disadvantage; 40 lb.

    ; heavy:
    :ring-mail {:ac (constantly 14)
               :disadvantage? :stealth}  ; 30 gp, 14— Disadvantage; 40 lb.
    :chain-mail {:ac (constantly 16)
                :min-str 13
                :disadvantage? :stealth}  ; 75 gp, 16, Str 13— Disadvantage; 55 lb.
    :splint {:ac (constantly 17)
             :min-str 15
             :disadvantage? :stealth}  ; 200 gp, 17, Str 15— Disadvantage; 60 lb.
    :plate {:ac (constantly 18)
            :min-str 15
            :disadvantage? :stealth}  ; 1500 gp, 18, Str 15— Disadvantage; 65 lb.

    ; shield:
    :shield {:ac-buff 2}  ; 10gp, +2; 6 lb
   })

(def armor-kinds (memoize
                   #(->kind-maps armor)))

(defn armor? [item]
  (and (= :armor (:type item))
       (not= :shield (:kind item))))

(defn shield? [item]
  (= :shield (:kind item)))

(def ^:private weapons
  {
   ; Simple Melee weapons
   :club {:damage :bludgeoning
          :dice "1d4"
          :light? true}
   :dagger {:damage :piercing
            :dice "1d4"
            :finesse? true
            :light? true
            :range [20 60]}
   :greatclub {:damage :bludgeoning
               :two-handed? true
               :dice "1d8"}
   :handaxe {:damage :slashing
             :dice "1d6"
             :light? true
             :range [20 60]}
   :javelin {:damage :piercing
             :dice "1d6"
             :range [30 120]}
   :light-hammer {:damage :bludgeoning
                  :dice "1d4"
                  :light? true
                  :range [20 60]}
   :mace {:damage :bludgeoning
          :dice "1d6"}
   :quarterstaff {:damage :bludgeoning
                  :dice "1d6"
                  :versatile "1d8"}
   :sickle {:damage :slashing
            :dice "1d4"
            :light? true}
   :spear {:damage :piercing
           :dice "1d6"
           :range [20 60]
           :versatile "1d8"}

   ; Simple Ranged Weapons
   :light-crossbow {:damage :piercing
                    :dice "1d8"
                    :two-handed? true
                    :uses-ammunition? true
                    :ranged? true
                    :range [80 320]}
   :dart {:damage :piercing
          :dice "1d4"
          :finesse? true
          :ranged? true
          :range [20 60]}
   :shortbow {:damage :piercing
              :two-handed? true
              :uses-ammunition? true
              :ranged? true
              :range [80 320]
              :dice "1d6"}
   :sling {:damage :bludgeoning
           :dice "1d4"
           :uses-ammunition? true
           :ranged? true
           :range [30 120]}

   ; Martial Melee Weapons
   :battleaxe {:damage :slashing
               :dice "1d8"
               :versatile "1d10"}
   :flail {:damage :bludgeoning
           :dice "1d8"}
   :glaive {:damage :slashing
            :heavy? true
            :reach? true
            :two-handed? true
            :dice "1d10"}
   :greataxe {:damage :slashing
              :heavy? true
              :two-handed? true
              :dice "1d12"}
   :greatsword {:damage :slashing
                :heavy? true
                :two-handed? true
                :dice "2d6"}
   :halberd {:damage :slashing
             :heavy? true
             :reach? true
             :two-handed? true
             :dice "1d10"}
   :lance {:damage :piercing
           :reach? true
           :special? true
           :dice "1d12"}
   :longsword {:damage :slashing
               :dice "1d8"
               :versatile "1d10"}
   :maul {:damage :bludgeoning
          :heavy? true
          :two-handed? true
          :dice "2d6"}
   :morningstar {:damage :piercing
                 :dice "1d8"}
   :pike {:damage :piercing
          :heavy? true
          :reach? true
          :two-handed? true
          :dice "1d10"}
   :rapier {:damage :piercing
            :finesse? true
            :dice "1d8"}
   :scimitar {:damage :piercing
              :finesse? true
              :light? true
              :dice "1d6"}
   :shortsword {:damage :piercing
                :finesse? true
                :light? true
                :dice "1d6"}
   :trident {:damage :piercing
             :range [20 60]
             :dice "1d6"
             :versatile "1d8"}
   :warpick {:damage :piercing
             :dice "1d8"}
   :warhammer {:damage :bludgeoning
               :dice "1d8"
               :versatile "1d10"}
   :whip {:damage :slashing
          :finesse? true
          :reach? true
          :dice "1d4"}

   ; Martial Ranged Weapons
   :blowgun {:damage :piercing
             :dice "1"
             :uses-ammunition? true
             :ranged? true
             :range [25 100]}
   :handcrossbow {:damage :piercing
                  :dice "1d6"
                  :uses-ammunition? true
                  :light? true
                  :ranged? true
                  :range [30 120]}
   :heavycrossbow {:damage :piercing
                   :dice "1d10"
                   :heavy? true
                   :two-handed? true
                   :uses-ammunition? true
                   :ranged? true
                   :range [100 400]}
   :longbow {:damage :piercing
             :dice "1d8"
             :heavy? true
             :two-handed? true
             :uses-ammunition? true
             :ranged? true
             :range [150 600]}
   :net {:special? true
         :range [5 15]}
   })

(def weapon-kinds (memoize
                    #(->kind-maps weapons)))

(defn inflate-armor
  [a]
  (let [opts (get armor (:kind a))]
    (-> a

        (as-> a
          (if-let [ac (:ac-buff opts)]
            (assoc-in a [:attrs :buffs :ac (:id a)] ac)
            a))

        (as-> a
          (if-let [ac (:ac opts)]
            (assoc-in a [:attrs :5e/ac (:id a)]
                      (if-let [ac-bonus (:+ a)]
                        (comp (partial + ac-bonus)
                              ac)
                        ac))
            a))

        (as-> a
          (if-let [skill (:disadvantage? opts)]
            (assoc-in a [:attrs :rolls skill :disadvantage]
                      (:name a))
            a)))))

(defn inflate-weapon
  [w]
  ; much easier
  (if-let [m (get weapons (:kind w))]
    (merge m w)
    w))

(defn inflate-by-type
  [{:keys [type] :as item}]
  (case type
    :armor (inflate-armor item)
    :weapon (inflate-weapon item)

    ; nothing to do
    item))


; ======= conditions =======================================

(def conditions
  {:blinded
   {:id :blinded
    :name "Blinded"
    :desc "• A blinded creature can't see and automatically fails any ability check that requires sight.
• Attack rolls against the creature have advantage, and the creature's attack rolls have disadvantage."}

   :charmed
   {:id :charmed
    :name "Charmed"
    :desc "• A charmed creature can't attack the charmer or target the charmer with harmful abilities or magical effects.
• The charmer has advantage on any ability check to interact socially with the creature."}

   :deafened
   {:id :deafened
    :name "Deafened"
    :desc "• A deafened creature can't hear and automatically fails any ability check that requires hearing."}

   :exhaustion
   {:id :exhaustion
    :name "Exhaustion"
    :levels 6
    :per-level {1 "Disadvantage on ability checks"
                2 "Speed halved"
                3 "Disadvantage on attack rolls and saving throws"
                4 "Hit point maximum halved"
                5 "Speed reduced to 0"
                6 "Death"}
    :desc "Some special abilities and environmental hazards, such as starvation and the long-term effects of freezing or scorching temperatures, can lead to a special condition called exhaustion. Exhaustion is measured in six levels. An effect can give a creature one or more levels of exhaustion, as specified in the effect's description.
    **Level  Effect**
      **1**    Disadvantage on ability checks
      **2**    Speed halved
      **3**    Disadvantage on attack rolls and saving throws
      **4**    Hit point maximum halved
      **5**    Speed reduced to 0
      **6**    Death

If an already exhausted creature suffers another effect that causes exhaustion, its current level of exhaustion increases by the amount specified in the effect's description.
A creature suffers the effect of its current level of exhaustion as well as all lower levels. For example, a creature suffering level 2 exhaustion has its speed halved and has disadvantage on ability checks.
An effect that removes exhaustion reduces its level as specified in the effect's description, with all exhaustion effects ending if a creature's exhaustion level is reduced below 1.
Finishing a long rest reduces a creature's exhaustion level by 1, provided that the creature has also ingested some food and drink."}

   :frightened
   {:id :frightened
    :name "Frightened"
    :desc "• A frightened creature has disadvantage on ability checks and attack rolls while the source of its fear is within line of sight.
• The creature can't willingly move closer to the source of its fear."}

   :grappled
   {:id :grappled
    :name "Grappled"
    :desc "• A grappled creature's speed becomes 0, and it can't benefit from any bonus to its speed.
• The condition ends if the grappler is incapacitated (see the condition).
• The condition also ends if an effect removes the grappled creature from the reach of the grappler or grappling effect, such as when a creature is hurled away by the _thunder wave_ spell."}

   :incapacitated
   {:id :incapacitated
    :name "Incapacitated"
    :desc "• An incapacitated creature can't take actions or reactions."}

   :invisible
   {:id :invisible
    :name "Invisible"
    :desc "• An invisible creature is impossible to see without the aid of magic or a special sense. For the purpose of hiding, the creature is heavily obscured. The creature's location can be detected by any noise it makes or any tracks it leaves.
• Attack rolls against the creature have disadvantage, and the creature's attack rolls have advantage."}

   :paralyzed
   {:id :paralyzed
    :name "Paralyzed"
    :desc "• A paralyzed creature is incapacitated (see the condition) and can't move or speak.
• The creature automatically fails Strength and Dexterity saving throws. Attack rolls against the creature have advantage.
• Any attack that hits the creature is a critical hit if the attacker is within 5 feet of the creature."}

   :petrified
   {:id :petrified
    :name "Petrified"
    :desc "• A petrified creature is transformed, along with any nonmagical object it is wearing or carrying, into a solid inanimate substance (usually stone). Its weight increases by a factor of ten, and it ceases aging.
• The creature is incapacitated (see the condition), can't move or speak, and is unaware of its surroundings.,
• Attack rolls against the creature have advantage.,
• The creature automatically fails Strength and Dexterity saving throws.,
• The creature has resistance to all damage.,
• The creature is immune to poison and disease, although a poison or disease already in its system is suspended, not neutralized."}

   :id
   {:id :poisoned
    :name "Poisoned"
    :desc "• A poisoned creature has disadvantage on attack rolls and ability checks."}

   :prone
   {:id :prone
    :name "Prone"
    :desc "• A prone creature's only movement option is to crawl, unless it stands up and thereby ends the condition.
• The creature has disadvantage on attack rolls.
• An attack roll against the creature has advantage if the attacker is within 5 feet of the creature. Otherwise, the attack roll has disadvantage."}

   :restrained
   {:id :restrained
    :name "Restrained"
    :desc "• A restrained creature's speed becomes 0, and it can't benefit from any bonus to its speed.
• Attack rolls against the creature have advantage, and the creature's attack rolls have disadvantage.
• The creature has disadvantage on Dexterity saving throws."}

   :stunned
   {:id :stunned
    :name "Stunned"
    :desc "• A stunned creature is incapacitated (see the condition), can't move, and can speak only falteringly.
• The creature automatically fails Strength and Dexterity saving throws.
• Attack rolls against the creature have advantage."}

  :unconscious
  {:id :unconscious
   :name "Unconscious"
   :desc "• An unconscious creature is incapacitated (see the condition), can't move or speak, and is unaware of its surroundings
• The creature drops whatever it's holding and falls prone.
• The creature automatically fails Strength and Dexterity saving throws.
• Attack rolls against the creature have advantage.
• Any attack that hits the creature is a critical hit if the attacker is within 5 feet of the creature."}})

(def conditions-sorted
  (memoize #(->> conditions
                 vals
                 (sort-by :name))))


; ======= skills ==========================================

(def skill-feature-ids #{:proficiency/acrobatics
                         :proficiency/animal-handling
                         :proficiency/arcana
                         :proficiency/athletics
                         :proficiency/deception
                         :proficiency/history
                         :proficiency/insight
                         :proficiency/intimidation
                         :proficiency/investigation
                         :proficiency/medicine
                         :proficiency/nature
                         :proficiency/perception
                         :proficiency/performance
                         :proficiency/persuasion
                         :proficiency/religion
                         :proficiency/sleight-of-hand
                         :proficiency/stealth
                         :proficiency/survival})
