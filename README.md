# Dictim

Dictim is a library for representing the [d2 language](https://github.com/terrastruct/d2) in Clojure/ Clojurescript. It uses vectors and maps to represent d2 elements and attributes.

Dictim is intented to allow d2 to be produced programmatically, and for d2 diagrams to be read back into your programs as data. There's also a formatter so that the d2 looks good.

## Release Information

Latest release:

[deps.edn](https://clojure.org/reference/deps_and_cli) dependency information:

As a git dep:

```clojure
io.github.judepayne/dictim {:git/tag "0.5.0" :git/sha "ad18510"}
``` 


## Docs

* [Wiki](https://github.com/judepayne/dictim/wiki)


## Basic usage

Let's round trip from dictim to d2, and back!

### From dictim to d2

dictim and d2 have three principle types of elements: shapes, connections and containers.

Here's an example of producing a d2 specifiction of a diagram with two shapes and a connection:

```clojure
user=> (use 'dictim.compile)
nil
user=> (d2 [:s1 "Shape 1"][:s2 "Shape 2"][:s1 "->" :s2 "reln"])
"s1: Shape 1\ns2: Shape 2\ns1 -> s2: reln"

```

When sent to the d2 CLI executable:

<img src="img/ex1.png" width="200">

### From d2 to dictim

let's turn the above d2 string back into dictim.

```clojure
user=> (use 'dictim.parse)
nil
user> (dictim "s1: Shape 1\ns2: Shape 2\ns1 -> s2: reln" :key-fn keyword)
([:s1 "Shape 1"] [:s2 "Shape 2"] [:s1 "->" :s2 "reln"])

```

For details on dictim syntax, the compile, parse and format operations, please see the wiki.


## License

Copyright © 2023 Jude Payne

Distributed under the [MIT License](http://opensource.org/licenses/MIT)
