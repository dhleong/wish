Formatting
==========

WISH supports rich text formatting with an extended subset of markdown.

## Bold and Italics

You can make **bold** text using `**two asterisks**` and _italic_ text using
`_underline_`.

## Lists

You can make unordered lists by prefixing each item with `-`:

```
- First
- Second
- Third
```

results in:

- First
- Second
- Third

Similarly, you can make ordered lists by prefixing with a number followed by a
period:

```
1. First
1. Second
1. Third
```

results in:

1. First
1. Second
1. Third

## Advanced formatting

For efficiency, this is where we diverge from Markdown. All of the above can be
used anywhere within a string (eg: `"**bold**"`), but since all of our data is
formatted in EDN, there's no reason for us to limit ourselves to string
formats.

So, anywhere you can use string formatting (eg: spell descriptions), you can
instead use a vector to get paragraphs:

```clojure
["Paragraph 1"
 "Paragraph 2"]
```

is equivalent to:

```clojure
"Paragraph 1

Paragraph 2"
```

### Tables

You can create tables by using vector wrapping. We may support markdown tables
in the future, but this is faster for now:

```clojure
["Before the table"

 {:headers ["Header 1" "Header 2"]
  :rows [["1:1", "1:2"]
         ["2:1", "2:2"]]}

 "After the table"]
```

Both the `:headers` key and the `:rows` key are optional, but you *must*
provide at least one of them. You can of course use any of the above string
formatting tools in the strings of a table.
