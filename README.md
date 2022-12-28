# Dictim

Dictim is a library for representing the [d2 language](https://github.com/terrastruct/d2) in Clojure/ Clojurescript. It uses vectors to represent d2 elements and maps and lists to represent an element's attributes.

Dictim is intented to allow d2 diagrams to be produced programmatically, and for d2 diagrams to be read back into your programs as data.

## Release Information

Latest release:

[deps.edn](https://clojure.org/reference/deps_and_cli) dependency information:

As a git dep:

```clojure
io.github.judepayne/dictim {:git/tag "v0.1.0" :git/sha "f51750f"}
``` 


## Docs

* [Wiki](https://github.com/judepayne/dictim/wiki)


## Basic usage

### From dictim to d2

dictim and d2 have three different types of elements: shapes, connections and containers.

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

The first item in an element is its key. Keys are important in d2 for referring to objects elsewhere in the d2, for example in the source and destination of the connection element above.

The label attribute is supplied in the 2nd position for a shape (or 4th for a connection). It is optional. The final item in the element is its attribute map of d2 rendering instructions, also optional. The label may be specified in this map rather than using the 2nd position shortcut, or it doesn't need to be specified at all, in which case the key will be used.

Layout of shapes from a clojure collection:

```clojure
user=> (def nodes '(:a :b :c))
#'user/nodes
user=> (apply d2 (mapv (fn [n] [n (name n)]) nodes))
"a: a\nb: b\nc: c\n"
```

<img src="img/ex2.png" width="400">

Container elements are like shapes but can nest any number of child elements at their end:

```clojure
user=> (d2 [:ctr1 "The nodes" [:a "First Shape"] [:n "Next Shape"]])
"ctr1: The nodes {\n  a: First Shape\n  n: Next Shape\n}\n"
```

<img src="img/ex3.png" width="400">


For the full syntax, please see the [wiki](https://github.com/judepayne/dictim/wiki).


### From d2 to dictim


*WIP*

```clojure
user=> (use 'dictim.parser)
nil
user=> (clj "jude payne : Child{shape: Dude\ntris: Guy};")
([:ctr
  [:key "jude payne "]
  [:label "Child"]
  [:attr [:d2-key [:d2-word "shape"]] [:val "Dude"]]
  [:shape [:key "tris"] [:label "Guy"]]])
```

Note - this is not yet dictim, just parsed results. still a ..

*WIP*

## Licsense

Copyright © 2023 Jude Payne

Distributed under the [MIT License](http://opensource.org/licenses/MIT)
