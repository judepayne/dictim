# dictim


Mac+Linux+Windows builds[![CircleCI](https://dl.circleci.com/status-badge/img/gh/judepayne/dictim/tree/main.svg?style=svg)](https://dl.circleci.com/status-badge/redirect/gh/judepayne/dictim/tree/main)

[![bb compatible](https://raw.githubusercontent.com/babashka/babashka/master/logo/badge.svg)](https://babashka.org)

dictim syntax is a nested edn/ json syntax for expressing a diagram. dictim is a library for compiling it to either [d2's](https://github.com/terrastruct/d2) or [Graphviz's](https://graphviz.org) text languages, and parsing any piece of d2 (back) into dictim syntax.

dictim supprts both Clojure and Babashka and comes as a library, a command line tool and a microservice (see dictim.server below). Clojurescript support is experimental.

## Rationale

Diagrams as data: dynamically generate rather than locking away information in hand produced diagrams. Dynamic diagrams create opportunities to better understand the information being visualized, e.g. group this way or that, style one way or another, dynamically include/ exclude information.

### Why parse d2 into dictim syntax?

Terrastruct, the company behind d2, have a commercial [diagramming IDE](https://terrastruct.com) ('D2 Studio') which allows you to build diagrams with d2 or by drag and drop and have the diagram and d2 stay in sync.

## Library Release Information

Latest release:

[deps.edn](https://clojure.org/reference/deps_and_cli) dependency information:

As a git dep:

```clojure
io.github.judepayne/dictim {:git/tag "0.9.0.3" :git/sha "3420f4d"}
```

d2 version compatibility: 0.7.0



## Docs & API

* [Wiki](https://github.com/judepayne/dictim/wiki)



## Basic usage

Let's round trip from dictim to d2, and back!

### From dictim to d2

dictim and d2 have three principle types of elements: shapes, connections and containers.

Here's an example of producing a d2 specifiction of a diagram with two shapes and a connection:

```clojure
user=> (use 'dictim.d2.compile)
nil
user=> (d2 [:s1 "Shape 1"][:s2 "Shape 2"][:s1 "->" :s2 "reln"])
"s1: Shape 1\ns2: Shape 2\ns1 -> s2: reln"

```

When sent to the d2 CLI executable:

<img src="img/ex1.png" width="250">

### From d2 to dictim

converting the d2 string back into dictim.

```clojure
user=> (use 'dictim.d2.parse)
nil
user> (dictim "s1: Shape 1\ns2: Shape 2\ns1 -> s2: reln" :key-fn keyword)
([:s1 "Shape 1"] [:s2 "Shape 2"] [:s1 "->" :s2 "reln"])

```

Using some more features from d2:

````clojure
user=>(use `dictim.d2.compile)
nil
user=>(d2 ["c1" "Container"
            ["s1" "Shape 1" {"shape" "circle"}]
            ["s3" "Companion"]]
          ["s2" "Shape 2"]
          ["c1.s1"  "->" "s2" "reln" {"style.stroke-dash" 3, "style.stroke" "deepskyblue"}])
"c1: Container {\n  s1: Shape 1 {shape: circle}\n  s3: Companion\n}\ns2: Shape 2\nc1.s1 -> s2: reln {\n  style.stroke-dash: 3\n  style.stroke: deepskyblue\n}"	  
````

<img src="img/ex3.svg" width="500">


dictim can easily be specified as json rather than clojure edn.

Whilst dictim syntax is nested, this library also offers dictim-flat syntax, a secondary unnested syntax variant. dictim-flat may be easier to produce and manipulate programmatically depending on your use case. dictim syntax and dictim-flat can be converted to each other in the `dictim.flat` namespace with the `flatten` and `build` functions.


For details on dictim syntax, the compile, parse and other operations, please see the [wiki](https://github.com/judepayne/dictim/wiki).


## Command line tool

dictim comes as a native build for mac, windows and linux, and as a babashka script. e.g.

````bash
dictim -c -w sample.edn -o out.d2
````

will watch the edn format dictim file `sample.edn` and compile it to d2 in `out.d2` whenever there's a change.


````bash
dictim -prj -w out.d2 -o out.json
````

will watch the d2 file `out.d2` and convert to json formet dictim whenever there's a change.


See the [wiki](https://github.com/judepayne/dictim/wiki/Command-Line) for details.


## Related Projects

This project is the base project for a number of other projects:

- [dictim.graph](https://github.com/judepayne/dictim.graph) Convert a representation of a graph into dictim: ideal for boxes and arrows/ network diagrams
- [dictim.cookbook](https://github.com/judepayne/dictim.cookbook) Examples of dictim in action!
- [dictim.server](https://github.com/judepayne/dictim.server) A easy-to-deploy microservice for converting dictim into d2 diagrams.


## License

Copyright © 2025 Jude Payne

Distributed under the [MIT License](http://opensource.org/licenses/MIT)
