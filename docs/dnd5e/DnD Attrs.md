D&D 5e-specific `:attrs`
========================

For different value types, such as that used for `:aoe`, see
[D&D Value Types](./DnD%20Values.md).

## `:action`

Declare that a feature can be used as an Action.

### Format

A map of `feature-id -> true`.

EX:

```clojure
{:id :ranger/primeval-awareness
 :name "Primeval Awareness"
 :desc "You can use your action and expend one ranger spell slot to focus your awareness on the region around you. For 1 minute per level of the spell slot you expend, you can sense whether the following types of creatures are present within 1 mile of you (or within up to 6 miles if you are in your favored terrain): aberrations, celestials, dragons, elementals, fey, fiends, and undead. This feature doesn’t reveal the creatures’ location or number."
 :! [[:!provide-attr
      [:action :ranger/primeval-awareness]
      true]]}
```

## `:attacks`

Provides extra attacks, such as the Dragonborn Breath Weapon.

### Format

A map of `id -> info` where `info` looks like:

```clojure
{:&from-option :option-id
 :name "Weapon Name"
 :consumes :limited-use-id
 :save :dex             ; or :str, :con, etc.; saving throw type
 :damage :acid          ; or :fire, etc.; damage type
 :aoe [:line 30, 5]     ; area of effect
 :dice (fn [level]
         "Calculated damage dice")
 :save-dc (fn [modifiers prof-bonus]
            (+ 8 (:con modifiers) prof-bonus))}
```

Here, `:&from-option` points to the id of an Option whose
values will be merged into the `info` map. This can be useful
with, for example, the Dragonborn Breath Weapon whose damage
type depends on the Dragonborn Ancestor feature's option.

EX:

```clojure
[:!provide-attr
 [:attacks :dragonborn/breath-weapon]
 {:&from-option :dragonborn/ancestry
  :name "Dragonborn Breath Weapon"
  :consumes :dragonborn/breath-weapon#uses
  :dice (fn [level]
          (cond
            (< level 6) "2d6"
            (< level 11) "3d6"
            (< level 16) "4d6"
            :else "5d6"))
  :save-dc (fn [modifiers prof-bonus]
             (+ 8 (:con modifiers) prof-bonus))}]
```

In the example above, the `:aoe`, `:damage`, and `:save` are provided by
the `:dragonborn/ancestry` option, eg:

```clojure
{:id :dragonborn/ancestry-black
 :name "Dragonborn: Black Dragon Ancestor"
 :desc "Your breath weapon does Acid damage"
 :save :dex
 :damage :acid
 :aoe [:line 30, 5]}
```

## `:attacks-per-action`

Provide a source of extra attacks.

### Format

A map of `id -> attacks` where `attacks` is a functional number value.

EX:

```clojure
[:!provide-attr
 [:attacks-per-action :extra-attack]
 2]

[:!provide-attr
 [:attacks-per-action :fighter/extra-attack]
 (fn [level]
   (cond
     (< level 11) 2
     (< level 20) 3
     :else 4))]
```


## `:bonus`

Declare that a feature can be used as a Bonus Action. See [`:action`](#action)
above for format and usage.

## `:buffs`

Provide buffs to various properties, like Armor Class or Saving Throws.

### Format

A nested map of `kind(s) -> id -> value` where `kind(s)` is one or more
keys (see below) and `value` depends on the kinds.

#### Constant buffs

The following kinds expect their `value` to be a constant number:

 - `:ac` Armor Class
 - `:atk :melee` Melee Attack Rolls
 - `:atk :ranged` Ranged Attack Rolls
 - `:saves` Saving Throws
 - `:checks` Ability Checks
 - `:spell-atk` Spell Attack Rolls
 - Abilities, like `:str`, `:dex`, etc.

#### Update buffs

If necessary, you can also functionally buff abilities using `:!update-attr`,
as follows:

```clojure
[:!update-attr [:buffs :str] inc]
```

This is uncomon.

#### Functional Number buffs

The following kinds can accept a functional number `value`:

 - `:hp-max` Max HP
 - `:speed` Speed, in feet

#### `:dmg :melee` and `:dmg :ranged`

Damage buffs. The value is a map that looks like:

```clojure
{:dice "1d6"        ; additional dice; can also be a function
 :type :radiant     ; or fire, etc.
 :when-two-handed?  ; if provided, the weapon's two-handedness must match
 :when-versatile    ; as above
 :+ 2               ; constant bonus
 }
```

EX:

```clojure
[:!provide-attr
 [:buffs :dmg :melee :fight/dueling-style]
 {:when-two-handed? false
  :when-versatile? false
  :+ 2}]
```

## `:combat-info`

Provides extra info that may be useful in combat, such as a Rogue's Sneak Attack Damage.

### Format

A map of `id -> info` where `info` looks like:

```clojure
{:name "Info label"
 :value (fn [level]
          "Calculated string value")}
```

EX:

```clojure
[:!provide-attr
 [:combat-info :rogue/sneak-attack]
 {:name "Sneak Attack Damage"
  :value (fn [level]
           (str (ceil (/ level 2))
            "d6"))}]
```

## `:immunities`

Provide information about special immunities. See [`:resistances`](#resistances).

## `:reaction`

Declare that a feature can be used as a Reaction. See [`:action`](#action)
above for format and usage.

## `:resistances`

Provide information about special resistances.

### Format

Map of `id -> {:desc}` where the value of `:desc` should be a string describing the resistance granted, or `id -> true` where `id` is the value of a feature whose
`:desc` will be used.

EX:

```clojure
[:!provide-attr [:resistances :dwarf/dwarven-resilience-resistances]
 {:desc "You have resistance against poison damage."}]
```

```clojure
[:!provide-attr [:immunities :paladin/divine-health] true]
```

## `:saves`

Provide information about special saving throw modifiers. See [`:resistances`](#resistances).

## `:special-action`

Declare that a feature can be used as a "Special Action," such as a Rogue's
Sneak Attack. Sneak Attack does not consume any time on its own, but is
used in conjunction with an action.
See [`:action`](#action) above for format and usage.

## `:spells`

Modify the attributes of a spell. Very commonly used by Warlock Eldritch Invocations, but might also be used by racial traits.

### Format

Map of `class-id -> spell-id -> modifiers` where `class-id` is the ID of the class or race providing the spell (EG: `:tiefling` or `:warlock`), `:spell-id` is the id of the spell to be modified, and `modifiers` is a map that looks like:

```clojure
{:spell-level 2             ; spell level to cast it at
 :upcast? false             ; disable other upcasting
 :consumes :limited-use-id  ; consume a limited-use instead of spell slots
 :at-will? true             ; make a spell be usable at-will
 }
```

All of these keys are optional, and you need only provide those that are necessary to your specific feature.
