D&D 5e-specific Value Types
===========================

## Area of Effect (`:aoe`)

This is a vector describing the area of effect of a spell or attack. The first item in the vector is the type, eg `:cylinder`, followed by one or two integers indicating the dimensions. See each specific type for more details. All dimensions are in feet

### `:cylinder`

```clojure
[:cylinder radius height]
```

### `:circle`

```clojure
[:circle radius]
```

### `:cone`

```clojure
[:cone length]
```

### `:sphere`

```clojure
[:sphere radius]
```

### `:cube`

```clojure
[:cube width]
```

### `:line`

```clojure
[:line length width]
```

### `:square`

```clojure
[:square width]
```
