# Dictim

[![CircleCI](https://dl.circleci.com/status-badge/img/gh/judepayne/dictim/tree/main.svg?style=svg)](https://dl.circleci.com/status-badge/redirect/gh/judepayne/dictim/tree/main)

Dictim is a library for representing Terrastruct's [d2](https://github.com/terrastruct/d2) and Graphviz's [dot](https://graphviz.org/doc/info/lang.html) languages in Clojure/ Clojurescript. It uses vectors and maps to represent d2 elements and attributes.

Dictim allows d2 and dot to be produced programmatically, and for d2 diagrams to be read back into your programs as data.

## Release Information

Latest release:

[deps.edn](https://clojure.org/reference/deps_and_cli) dependency information:

As a git dep:

```clojure
io.github.judepayne/dictim {:git/tag "0.5.7" :git/sha "9277dfc"}
``` 


## Docs

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

<img src="img/ex1.png" width="200">

### From d2 to dictim

let's turn the above d2 string back into dictim.

```clojure
user=> (use 'dictim.d2.parse)
nil
user> (dictim "s1: Shape 1\ns2: Shape 2\ns1 -> s2: reln" :key-fn keyword)
([:s1 "Shape 1"] [:s2 "Shape 2"] [:s1 "->" :s2 "reln"])

```

For details on dictim syntax, the compile, parse and format operations, please see the [wiki](https://github.com/judepayne/dictim/wiki).


This project is the base project for a number of other projects:

- [dictim.graph](https://github.com/judepayne/dictim.graph) Convert a representation of a graph into dictim: ideal for boxes and arrows/ network diagrams
- [dictim.cookbook](https://github.com/judepayne/dictim.cookbook) Examples of dictim in action!
- [dictim.server](https://github.com/judepayne/dictim.server) A easy-to-deploy microservice for converting dictim into d2 diagrams.

## d2 version compatibility

dictim is currently compatible with d2 v0.2.4 (and earlier).
We plan to bring this up to d2 v0.6.3 soon!


## License

Copyright © 2024 Jude Payne

Distributed under the [MIT License](http://opensource.org/licenses/MIT)
