(ns dictim.d2.compile-test
  (:require [clojure.test :refer :all]
            [dictim.d2.compile :as c]))

;; compilation

(def ex-graph
  [:jude "Friends"
   [:tristram "T Biggs expanded"
    [:tris "TAB"]
    [:maddie "Madeline"]
    [:tris "--" :maddie "wedding bells?"]
    [:tris "->" :children "previously sired"]
    [:children "the brood"
     ["Oliver" "Eldest" {:style {:fill "orange"}}]
     ["Roof" "Good footballer" {:shape "person"}]]]])


(def ex-graph-d2 "jude: Friends  {\n  tristram: T Biggs expanded  {\n    tris: TAB\n    maddie: Madeline\n    tris -- maddie: wedding bells?\n    tris -> children: previously sired\n    children: the brood  {\n      Oliver: Eldest  {\n        style:  {\n          fill: orange\n        }\n      }\n      Roof: Good footballer  {\n        shape: person\n      }\n    }\n  }\n}")


(deftest basic-compilation
  (testing "Compiling basic dictim to d2."
    (is (= (c/d2 ex-graph) ex-graph-d2))))


(def ex-sequence
  [:various
   "demonstrate various things"
   [:block
    "Block Text - e.g. Markdown"
    [:a "|md ## A header |"]
    [:b "|md ### a smaller header |" {:shape :cloud}]
    [:c "|md *some italic text* |"]
    [:a "--" :b "->" :c]]
   [:class
    "Class diagram"
    {"shape" "class"}
    ["\\#reader" "io.RuneReader"]
    ["method(a unint64)" "(x, y, int)"]
    ["-lookahead" "'[]rune'"]]              ;; <-- best way to escape strings for d2
   ["convs"
    "Office Conversations"
    ["conv1"
     "Office conversation 1"
     {"shape" "sequence_diagram"}
     [:comment "This is a comment"]
     [:list "bob" "alice"]
     ["alice" "Alice" {"shape" "person", "style" {"fill" "orange"}}]
     ["bob.\"In the eyes of my (dog), I'm a man.\""]
     ["awkward small talk"
      ["alice" "->" "bob" "um, hi"]
      ["bob" "->" "alice" "oh, hello"]
      ["icebreaker attempt"
       ["alice" "->" "bob" "what did you have for lunch?"]]
      ["fail"
       {"style" {"fill" "green"}}
       ["bob" "->" "alice" "that's personal"]]]]
    ["conv2"
     "Office conversation 2"
     {"shape" "sequence_diagram"}
     [:list "simon" "trev"]
     ["simon" "Simon" {"shape" "person"}]
     ["trev" "Trevor"]
     ["failed conversation"
      ["simon" "->" "trev" "seen the football"]
      ["trev" "->" "simon" "no, I was at my gran's"]
      ["Carry on anyway"
       ["simon" "->" "trev" "mate, you missed a classic"]]]]
    ["conv1" "->" "conv2" "spot the difference?"]]])


(def ex-sequence-d2
  "various: demonstrate various things  {\n  block: Block Text - e.g. Markdown  {\n    a: |md ## A header |\n    b: |md ### a smaller header |  {\n      shape: cloud\n    }\n    c: |md *some italic text* |\n    a -- b -> c\n  }\n  class: Class diagram  {\n    shape: class\n    \\#reader: io.RuneReader\n    method(a unint64): (x, y, int)\n    -lookahead: '[]rune'\n  }\n  convs: Office Conversations  {\n    conv1: Office conversation 1  {\n      shape: sequence_diagram\n      # This is a comment\n      bob;alice\n      alice: Alice  {\n        shape: person\n        style:  {\n          fill: orange\n        }\n      }\n      bob.\"In the eyes of my (dog), I'm a man.\"\n      awkward small talk:   {\n        alice -> bob: um, hi\n        bob -> alice: oh, hello\n        icebreaker attempt:   {\n          alice -> bob: what did you have for lunch?\n        }\n        fail:   {\n          style:  {\n            fill: green\n          }\n          bob -> alice: that's personal\n        }\n      }\n    }\n    conv2: Office conversation 2  {\n      shape: sequence_diagram\n      simon;trev\n      simon: Simon  {\n        shape: person\n      }\n      trev: Trevor\n      failed conversation:   {\n        simon -> trev: seen the football\n        trev -> simon: no, I was at my gran's\n        Carry on anyway:   {\n          simon -> trev: mate, you missed a classic\n        }\n      }\n    }\n    conv1 -> conv2: spot the difference?\n  }\n}")


(deftest complex-compilation
  (testing "Compiling a complex sequence diagram to d2."
    (is (= (c/d2 ex-sequence) ex-sequence-d2))))


(def process-diagram
  '(["Process View"
     {:style.fill "'#f7f6f5'"}
     ["p113"
      {:style.fill "'#f5f0e1'"}
      ["app14148" "Flame" {:shape "hexagon", :style {:fill "green"}}]
      ["app14154" "Data Solar" {:shape "hexagon", :style {}}]
      ["app14156" "Data sky" {:shape "hexagon", :style {}}]]
     ["p114"
      {:style.fill "'#f5f0e1'"}
      ["app14149"
       "Data Solar"
       {:shape "hexagon", :style {:fill "pink"}}]
      ["app14150"
       "Flame minor"
       {:shape "hexagon", :style {:fill "pink"}}]
      ["app14153" "ARC3" {:shape "hexagon", :style {:fill "pink"}}]
      ["app14155"
       "Risk Sheet"
       {:shape "hexagon", :style {:fill "pink"}}]]
     ["p112"
      {:style.fill "'#f5f0e1'"}
      ["app14147" "ARC3" {:shape "hexagon", :style {:fill "orange"}}]
      ["app14151" "Booking Flash" {:shape "hexagon", :style {}}]
      ["app14152" "Book Relay" {:shape "hexagon", :style {}}]]]
    ["Process View.p114.app14155"
     "->"
     "Process View.p114.app14153"
     "client master"
     {:style.stroke "'#4eb8ed'",
      :style.stroke-width "4",
      :style.font-size "16",
      :style.font-color "red"}]
    ["Process View.p113.app14154"
     "->"
     "Process View.p114.app14149"
     "client master"
     {:style.stroke "'#4eb8ed'",
      :style.stroke-width "4",
      :style.font-size "16",
      :style.font-color "red"}]
    ["Process View.p114.app14155"
     "->"
     "Process View.p113.app14148"
     "client master"
     {:style.stroke "'#4eb8ed'",
      :style.stroke-width "4",
      :style.font-size "16",
      :style.font-color "red"}]
    ["Process View.p113.app14156"
     "->"
     "Process View.p112.app14152"
     "client master"
     {:style.stroke "'#4eb8ed'",
      :style.stroke-width "4",
      :style.font-size "16",
      :style.font-color "red"}]
    ["Process View.p114.app14155"
     "->"
     "Process View.p112.app14147"
     "client master"
     {:style.stroke "'#4eb8ed'",
      :style.stroke-width "4",
      :style.font-size "16",
      :style.font-color "red"}]
    ["Process View.p114.app14155"
     "->"
     "Process View.p112.app14147"
     "client master"
     {:style.stroke "'#4eb8ed'",
      :style.stroke-width "4",
      :style.font-size "16",
      :style.font-color "red"}]))


(def process-diagram-d2
  "Process View:   {\n  style.fill: '#f7f6f5'\n  p113:   {\n    style.fill: '#f5f0e1'\n    app14148: Flame  {\n      shape: hexagon\n      style:  {\n        fill: green\n      }\n    }\n    app14154: Data Solar  {\n      shape: hexagon\n    }\n    app14156: Data sky  {\n      shape: hexagon\n    }\n  }\n  p114:   {\n    style.fill: '#f5f0e1'\n    app14149: Data Solar  {\n      shape: hexagon\n      style:  {\n        fill: pink\n      }\n    }\n    app14150: Flame minor  {\n      shape: hexagon\n      style:  {\n        fill: pink\n      }\n    }\n    app14153: ARC3  {\n      shape: hexagon\n      style:  {\n        fill: pink\n      }\n    }\n    app14155: Risk Sheet  {\n      shape: hexagon\n      style:  {\n        fill: pink\n      }\n    }\n  }\n  p112:   {\n    style.fill: '#f5f0e1'\n    app14147: ARC3  {\n      shape: hexagon\n      style:  {\n        fill: orange\n      }\n    }\n    app14151: Booking Flash  {\n      shape: hexagon\n    }\n    app14152: Book Relay  {\n      shape: hexagon\n    }\n  }\n}\nProcess View.p114.app14155 -> Process View.p114.app14153: client master  {\n  style.stroke: '#4eb8ed'\n  style.stroke-width: 4\n  style.font-size: 16\n  style.font-color: red\n}\nProcess View.p113.app14154 -> Process View.p114.app14149: client master  {\n  style.stroke: '#4eb8ed'\n  style.stroke-width: 4\n  style.font-size: 16\n  style.font-color: red\n}\nProcess View.p114.app14155 -> Process View.p113.app14148: client master  {\n  style.stroke: '#4eb8ed'\n  style.stroke-width: 4\n  style.font-size: 16\n  style.font-color: red\n}\nProcess View.p113.app14156 -> Process View.p112.app14152: client master  {\n  style.stroke: '#4eb8ed'\n  style.stroke-width: 4\n  style.font-size: 16\n  style.font-color: red\n}\nProcess View.p114.app14155 -> Process View.p112.app14147: client master  {\n  style.stroke: '#4eb8ed'\n  style.stroke-width: 4\n  style.font-size: 16\n  style.font-color: red\n}\nProcess View.p114.app14155 -> Process View.p112.app14147: client master  {\n  style.stroke: '#4eb8ed'\n  style.stroke-width: 4\n  style.font-size: 16\n  style.font-color: red\n}")


(deftest removing-empty-maps-compilation
  (testing "Compiling dicitm with empty maps that must be filtered out."
    (is (= (apply c/d2 process-diagram) process-diagram-d2))))


(deftest shape-keys-can-be-numbers
  (testing "Compiling dictim shapes with keys that are numbers."
    (is (= (c/d2 [1 "Person A"] [4 "Person C"] [1 "->" 2] [3 "->" 4 "->" 5])
           "1: Person A\n4: Person C\n1 -> 2\n3 -> 4 -> 5"))))


(deftest fill-pattern
  (testing "fill-pattern"
    (is (= (c/d2 [1 "->" 2 {:style {:fill-pattern "lines"}}])
           "1 -> 2:  {\n  style:  {\n    fill-pattern: lines\n  }\n}"))))


(deftest classes
  (testing ".class key endings"
    (is (= (apply c/d2 '({"direction" "right"}
                         ["classes"
                          ["load balancer"
                           {"label" "load\\nbalancer",
                            "width" 100,
                            "height" 200,
                            "style"
                            {"stroke-width" 0,
                             "fill" "\"#44C7B1\"",
                             "shadow" true,
                             "border-radius" 5}}]
                          ["unhealthy"
                           {"style" {"fill" "\"#FE7070\"", "stroke" "\"#F69E03\""}}]]
                         ["web traffic" "->" "web lb"]
                         ["web lb.class" "load balancer"]
                         ["web lb" "->" "api1"]
                         ["web lb" "->" "api2"]
                         ["web lb" "->" "api3"]
                         ["api2.class" "unhealthy"]
                         ["api1" "->" "cache lb"]
                         ["api3" "->" "cache lb"]
                         ["cache lb.class" "load balancer"]))
           "direction: right\nclasses:   {\n  load balancer:  {\n    label: load\\nbalancer\n    width: 100\n    height: 200\n    style:  {\n      stroke-width: 0\n      fill: \"#44C7B1\"\n      shadow: true\n      border-radius: 5\n    }\n  }\n  unhealthy:  {\n    style:  {\n      fill: \"#FE7070\"\n      stroke: \"#F69E03\"\n    }\n  }\n}\nweb traffic -> web lb\nweb lb.class: load balancer\nweb lb -> api1\nweb lb -> api2\nweb lb -> api3\napi2.class: unhealthy\napi1 -> cache lb\napi3 -> cache lb\ncache lb.class: load balancer"))))


(deftest globs
  (testing "unquoted globs are allowed in attribute keys."
    (is (= (apply c/d2 '(["foods"
                          ["pizzas"
                           ["cheese"]
                           ["sausage"]
                           ["pineapple"]
                           {"*.shape" "circle"}]
                          ["humans" ["john"] ["james"] {"*.shape" "person"}]
                          ["humans.*" "->" "pizzas.pineapple" "eats"]]))
           "foods:   {\n  pizzas:   {\n    cheese\n    sausage\n    pineapple\n    *.shape: circle\n  }\n  humans:   {\n    john\n    james\n    *.shape: person\n  }\n  humans.* -> pizzas.pineapple: eats\n}")))
  (testing "recursive globs"
    (is (= (apply c/d2 '(["a" ["b" ["c"]]] {"**.style.border-radius" 7}))
           "a:   {\n  b:   {\n    c\n  }\n}\n**.style.border-radius: 7")))
  (testing "unquoted globs not allowed in shape keys"
    (is (thrown? Exception
                 (c/d2 ["ha*lo" "The Halo"]))))
  (testing "quoted globs are allowed in shape keys"
    (is (= (c/d2 ["'ha*lo'" "The Halo"])
           "'ha*lo': The Halo")))
  (testing "unquoted globs are allowed in connection keys"
    (is (= (c/d2 ["*" "->" "y"])
           "* -> y"))))


(deftest connection-ref-attrs
  (testing "connection references as attr keys"
    (is (= (apply c/d2 '([1 "one" {:style.fill "green"}]
                         [2 "two" {:style.fill "blue"}]
                         [1 "->" 2]
                         {[1 "->" 2 [0]] {:style {:stroke "red"}}}))
           "1: one  {\n  style.fill: green\n}\n2: two  {\n  style.fill: blue\n}\n1 -> 2\n(1 -> 2)[0]:  {\n  style:  {\n    stroke: red\n  }\n}"))))



(deftest periods
  (testing "unquoted periods are allowed in shape keys"
    (is (= (c/d2 ["ha.lo" "The Halo"])
           "ha.lo: The Halo")))
  (testing "quoted periods are allowed in shape keys"
    (is (= (c/d2 ["ha'.l'o" "The Halo"])
           "ha'.l'o: The Halo"))))


(deftest vars
  (testing "vars are attributes"
    (is (= (apply c/d2 '({:direction :right}
                         {:vars {:server-name "Cat"}}
                         [:server1 "${server-name}-1"]
                         [:server2 "${server-name}-2"]
                         [:server1 "<->" :server2]))
           "direction: right\nvars:  {\n  server-name: Cat\n}\nserver1: ${server-name}-1\nserver2: ${server-name}-2\nserver1 <-> server2"))
    (testing "and cannot be used as shape keys."
        (is (thrown? Exception
                     (apply c/d2 '({:direction :right}
                                   [:vars [:server-name "Cat"]]
                                   [:server1 "${server-name}-1"]
                                   [:server2 "${server-name}-2"]
                                   [:server1 "<->" :server2])))))))


(deftest null-overrides
  (testing "attributes can have a nil/ null value"
    (is (= (c/d2 {:style.fill nil})
           "style.fill: null")))
  (testing "containers/ shapes can have a null label"
    (is (= (c/d2 [:shapeA nil [:shapeB]])
           "shapeA: null  {\n  shapeB\n}")))
  (testing "connection-references can be nulled"
    (is (= (c/d2 {[:a "->" :b [0]] nil})
           "(:a -> :b)[0]: null")))
  (testing "single connections can be nulled"
    (is (= (c/d2 [:x "->" :y nil])
           "x -> y: null")))
  (testing "multiple connections can be nulled"
    (is (= (c/d2 [:x "->" :y "--" :z nil])
           "x -> y -- z: null"))))
