D&D 5e-specific `:attrs`
========================

For different value types, such as that used for `:aoe`, see
[D&D Value Types](./Dnd Values.md).

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

## `:bonus`

Declare that a feature can be used as a Bonus Action. See [`:action`](#action)
above for format and usage.

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

## `:reaction`

Declare that a feature can be used as a Reaction. See [`:action`](#action)
above for format and usage.

## `:special-action`

Declare that a feature can be used as a "Special Action," such as a Rogue's
Sneak Attack. Sneak Attack does not consume any time on its own, but is
used in conjunction with an action.
See [`:action`](#action) above for format and usage.
