D&D 5e-specific `:attrs`
========================

## `:combat-info`

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
