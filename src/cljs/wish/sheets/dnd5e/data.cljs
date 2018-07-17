(ns ^{:author "Daniel Leong"
      :doc "Fixed 5e SRD data to avoid too much dup
            in the data sources"}
  wish.sheets.dnd5e.data)

(defn- ac<-dex+
  "Returns a fn that computes AC given dex plus
   the given `ac-base` value."
  ([ac-base]
   (ac<-dex+ ac-base 10)) ; basically, no limit on the modifier
  ([ac-base max-modifier]
   (fn [{{dex :dex} :modifiers}]
     (+ ac-base (min max-modifier
                     dex)))))

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
