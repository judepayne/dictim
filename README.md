# Dictim

Dictim is a library for representing the [d2 language](https://github.com/terrastruct/d2) in Clojure/ Clojurescript. It uses vectors and maps to represent d2 elements and attributes.

Dictim is intented to allow d2 diagrams to be produced programmatically, and for d2 diagrams to be read back into your programs as data.

## Release Information

Latest release:

[deps.edn](https://clojure.org/reference/deps_and_cli) dependency information:

As a git dep:

```clojure
io.github.judepayne/dictim {:git/tag "v0.2.0" :git/sha "f51750f"}
``` 


## Docs

* [Wiki](https://github.com/judepayne/dictim/wiki)


## Basic usage

### From dictim to d2

dictim and d2 have three principle types of elements: shapes, connections and containers.

Here's an example of producing a d2 specifiction of a diagram with two shapes and a connection:

```clojure
user=> (use 'dictim.compiler)
nil
user=> (d2 [:s1 "Shape 1"]
           [:s2 {:label "Shape 2"}]
	   [:s1 "->" :s2 {:label "rel'n"}])
```

Output with indentation:
```text
s1: Shape 1
s2: {
  label: Shape 2
}
s1 -> s2: {
  label: rel'n
}
```

When sent to the d2 CLI executable:

<img src="img/ex1.png" width="200">

The first item in an element is always its key. Keys are important in d2 for referring to objects elsewhere in the d2, for example in the source and destination of the connection element above.

An optional label goes in the next position of the vector for shapes and containers (or fourth position for a connection). The next and final element for shapes and connections is an optional map of d2 rendering instructions. Containers alone have additional elements after that; the set of other elements that they hold. Please see the wiki for more details on element format.

Layout of shapes from a clojure collection:

```clojure
user=> (def nodes '(:a :b :c))
#'user/nodes
user=> (apply d2 (mapv (fn [n] [n (name n)]) nodes))
"a: a\nb: b\nc: c\n"
```

<img src="img/ex2.png" width="400">

A container elements with two nested shapes:

```clojure
user=> (d2 [:ctr1 "The nodes" [:a "First Shape"] [:n "Next Shape"]])
"ctr1: The nodes {\n  a: First Shape\n  n: Next Shape\n}\n"
```

<img src="img/ex3.png" width="400">


For the full syntax, please see the [wiki](https://github.com/judepayne/dictim/wiki).

### Formatting

Compiling dictim to d2 produces well formatted output. The formatting can be controlled by passing a map of options as the first argument to the d2 function. The following keys make up the map:

 - `:separator`  default '\n'. The character used to separate d2 expressions.
 - `:format?`    default: true
 - `:tab`        default: 2. The indentation step used in formatting.

The formatter is also available separately via the `fmt` function in the `dictim.format` namespace to format existing d2 strings.


### From d2 to dictim

d2 strings can be parsed back into their dictim Clojure representation with the `clj` function in the `dictim.parser` namespace.

```clojure
user=> (use 'dictim.parser)
nil
user=> (clj "s1: Shape 1; s2: {label: Shape 2}; s1 -> s2: {label: rel'n}"
            :key-fn keyword)

([:s1 "Shape 1"]
 [:s2 "" [:label "Shape 2"]]
 [:s1 "->" :s2 "" {:label "rel'n"}])

```

Pease see the wiki for further details.

## Licsense

Copyright © 2023 Jude Payne

Distributed under the [MIT License](http://opensource.org/licenses/MIT)
