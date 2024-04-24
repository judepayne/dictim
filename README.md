# Dictim

[![CircleCI](https://dl.circleci.com/status-badge/img/gh/judepayne/dictim/tree/main.svg?style=svg)](https://dl.circleci.com/status-badge/redirect/gh/judepayne/dictim/tree/main)

[![bb compatible](https://raw.githubusercontent.com/babashka/babashka/master/logo/badge.svg)](https://babashka.org)

Diagrams as data: dynamically generate rather than locking away information in hand produced diagrams.

Dictim let's you specify diagrams as data, using Clojure's edn data format (or Json). You can compile the specification to 
[d2's](https://github.com/terrastruct/d2) text language and use d2 to convert that into a diagram. It's also possible to parse d2 back into edn.

This is a Clojure/Babashka library with experimental Clojurescript support.

Dictim also comes as a command line tool. Details below.

Dictim can also be compiled into Graphviz' dot text language.


## Release Information

Latest release:

[deps.edn](https://clojure.org/reference/deps_and_cli) dependency information:

As a git dep:

```clojure
io.github.judepayne/dictim {:git/tag "0.6.7" :git/sha "0ae6788"}
``` 

d2 version compatibility: 0.6.4


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

<img src="img/ex1.png" width="250">

### From d2 to dictim

let's turn the above d2 string back into dictim.

```clojure
user=> (use 'dictim.d2.parse)
nil
user> (dictim "s1: Shape 1\ns2: Shape 2\ns1 -> s2: reln" :key-fn keyword)
([:s1 "Shape 1"] [:s2 "Shape 2"] [:s1 "->" :s2 "reln"])

```

For details on dictim syntax, the compile, parse and other operations, please see the [wiki](https://github.com/judepayne/dictim/wiki).


## Command line tool

This project contains a command line tool version (a babashka script) that you can install and use to play with dictim on the command line. This makes to easy to create a toolchain that goes directly from dictim edn to a diagram. See the [wiki](https://github.com/judepayne/dictim/wiki/Command-Line) for details.


## Related Projects

This project is the base project for a number of other projects:

- [dictim.graph](https://github.com/judepayne/dictim.graph) Convert a representation of a graph into dictim: ideal for boxes and arrows/ network diagrams
- [dictim.cookbook](https://github.com/judepayne/dictim.cookbook) Examples of dictim in action!
- [dictim.server](https://github.com/judepayne/dictim.server) A easy-to-deploy microservice for converting dictim into d2 diagrams.


## License

Copyright © 2024 Jude Payne

Distributed under the [MIT License](http://opensource.org/licenses/MIT)
