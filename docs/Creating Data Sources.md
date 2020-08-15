Creating Data Sources
=====================

This doc is intended to provide guidance on creating dynamic data sources to
power a sheet implementation using everything WISH has to offer. The
[wish-engine Scripting API docs][1] are assumed prior reading.

As we currently only have one sheet implementation, for D&D 5th Edition, most
examples here will refer to that system, but in general the same capabilities
could be applied to future implementations for other systems.

## Consuming Limited-Use from a Feature

Often, a Feature can be only used only a limited number of times per
adventuring day. This is expressed by [providing a Limited Use item][2] so
players can track how many times they've used that feature. To take this
utility to the next level, you can also indicate which Limited Use a Feature
uses, enabling the sheet to show a convenient "Use" button wherever the
Feature is displayed on the sheet. To do this, simply add the `:consumes` key
to the Feature, providing the Limited Use ID as its value:

```clojure
; ...
(provide-feature
  {:id :sorcerer/font-of-magic
   ; ...
   :consumes :sorcerer/points#uses
   :! (on-state
        (provide-limited-use
          {:id :sorcerer/points#uses
           :name "Sorcery Points"
           ; ...
           }))})
```

Piece of cake.

### Consuming a spell slot (5e)

For D&D 5e Data Sources specifically, some features (such as a Paladin's
Divine Smite) actually consume spell slots. To support this, instead of
specifying a Limited Use ID you can provide the special key `:*spell-slot`.

### Specifying how many uses a feature consumes at a time.

By default, it is assumed that a feature will consume one or more uses of a
Limited Use, and the UI will provide something like a "Use 1" button. In some
cases, however, a Feature may use more than one, or a variable number. In such
cases, you can provide `:consumes/amount` to declare that. For example, the
Quicken Spell metamagic consumes 2 Sorcery Points:

```clojure
; ...
(declare-options
  :sorcerer/metamagic
  {:id :sorcerer/metamagic-quickened,
   :name "Quickened Spell",
   :desc
   "When you cast a spell that has a casting time of 1 action, you can spend 2 sorcery points to change the casting time to 1 bonus action for this casting."
   :consumes :sorcerer/points#uses
   :consumes/amount 2})
```

Piece of cake.

## Unrestricted options selection

This is probably an unusual case, but, for example, Wish provides a feature
called `:feats` which allows users to add as many Feats to their character as
they like, for facilitating homebrew situations.  Normally, Wish will
helpfully count how many options you've selected and indicate that against the
max allowed for a feature. Since `:feats` effectively has no max, it is marked
with `:unrestricted-options? true` so that we don't show that UI.


[1]: https://cljdoc.org/d/wish-engine/wish-engine/0.1.0-SNAPSHOT/api/wish-engine.scripting-api
[2]: https://cljdoc.org/d/wish-engine/wish-engine/0.1.0-SNAPSHOT/api/wish-engine.scripting-api#provide-limited-use
