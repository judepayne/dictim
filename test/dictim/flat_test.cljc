(ns dictim.flat-test
  (:require [clojure.test :refer :all]
            [dictim.flat :as f]))


(def ex
  '([:various
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
   ["-lookahead" "'[]rune'"]]
  ["convs"
   "Office Conversations"
   ["conv1"
    "Office conversation 1"
    {"shape" "sequence_diagram"}
    "#This is a comment"
    [:list ["bob"] ["alice"]]
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
    [:list ["simon"] ["trev"]]
    ["simon" "Simon" {"shape" "person"}]
    ["trev" "Trevor"]
    ["failed conversation"
     ["simon" "->" "trev" "seen the football"]
     ["trev" "->" "simon" "no, I was at my gran's"]
     ["Carry on anyway"
      ["simon" "->" "trev" "mate, you missed a classic"]]]]
   ["conv1" "->" "conv2" "spot the difference?"]]]))


(def ex-flat
  '({:type :ctr,
     :key :various,
     :meta {:label "demonstrate various things"},
     :parent nil}
    {:type :ctr,
     :key :block,
     :meta {:label "Block Text - e.g. Markdown"},
     :parent :various}
    {:type :shape,
     :key :a,
     :meta {:label "|md ## A header |"},
     :parent :block}
    {:type :shape,
     :key :b,
     :meta
     {:label "|md ### a smaller header |", :attrs {:shape :cloud}},
     :parent :block}
    {:type :shape,
     :key :c,
     :meta {:label "|md *some italic text* |"},
     :parent :block}
    {:type :conn,
     :key [:a "--" :b "->" :c],
     :meta {:label nil},
     :parent :block}
    {:type :ctr,
     :key :class,
     :meta {:label "Class diagram", :attrs {"shape" "class"}},
     :parent :various}
    {:type :shape,
     :key "\\#reader",
     :meta {:label "io.RuneReader"},
     :parent :class}
    {:type :shape,
     :key "method(a unint64)",
     :meta {:label "(x, y, int)"},
     :parent :class}
    {:type :shape,
     :key "-lookahead",
     :meta {:label "'[]rune'"},
     :parent :class}
    {:type :ctr,
     :key "convs",
     :meta {:label "Office Conversations"},
     :parent :various}
    {:type :ctr,
     :key "conv1",
     :meta
     {:label "Office conversation 1",
      :attrs {"shape" "sequence_diagram"}},
     :parent "convs"}
    {:type :cmt, :key "#This is a comment", :meta nil, :parent "conv1"}
    {:type :list, :key ("bob" "alice"), :meta nil, :parent "conv1"}
    {:type :shape,
     :key "bob",
     :meta {:label nil},
     :parent ("bob" "alice")}
    {:type :shape,
     :key "alice",
     :meta {:label nil},
     :parent ("bob" "alice")}
    {:type :shape,
     :key "alice",
     :meta
     {:label "Alice",
      :attrs {"shape" "person", "style" {"fill" "orange"}}},
     :parent "conv1"}
    {:type :shape,
     :key "bob.\"In the eyes of my (dog), I'm a man.\"",
     :meta {:label nil},
     :parent "conv1"}
    {:type :ctr,
     :key "awkward small talk",
     :meta {:label nil},
     :parent "conv1"}
    {:type :conn,
     :key ["alice" "->" "bob"],
     :meta {:label "um, hi"},
     :parent "awkward small talk"}
    {:type :conn,
     :key ["bob" "->" "alice"],
     :meta {:label "oh, hello"},
     :parent "awkward small talk"}
    {:type :ctr,
     :key "icebreaker attempt",
     :meta {:label nil},
     :parent "awkward small talk"}
    {:type :conn,
     :key ["alice" "->" "bob"],
     :meta {:label "what did you have for lunch?"},
     :parent "icebreaker attempt"}
    {:type :ctr,
     :key "fail",
     :meta {:attrs {"style" {"fill" "green"}}},
     :parent "awkward small talk"}
    {:type :conn,
     :key ["bob" "->" "alice"],
     :meta {:label "that's personal"},
     :parent "fail"}
    {:type :ctr,
     :key "conv2",
     :meta
     {:label "Office conversation 2",
      :attrs {"shape" "sequence_diagram"}},
     :parent "convs"}
    {:type :list, :key ("simon" "trev"), :meta nil, :parent "conv2"}
    {:type :shape,
     :key "simon",
     :meta {:label nil},
     :parent ("simon" "trev")}
    {:type :shape,
     :key "trev",
     :meta {:label nil},
     :parent ("simon" "trev")}
    {:type :shape,
     :key "simon",
     :meta {:label "Simon", :attrs {"shape" "person"}},
     :parent "conv2"}
    {:type :shape,
     :key "trev",
     :meta {:label "Trevor"},
     :parent "conv2"}
    {:type :ctr,
     :key "failed conversation",
     :meta {:label nil},
     :parent "conv2"}
    {:type :conn,
     :key ["simon" "->" "trev"],
     :meta {:label "seen the football"},
     :parent "failed conversation"}
    {:type :conn,
     :key ["trev" "->" "simon"],
     :meta {:label "no, I was at my gran's"},
     :parent "failed conversation"}
    {:type :ctr,
     :key "Carry on anyway",
     :meta {:label nil},
     :parent "failed conversation"}
    {:type :conn,
     :key ["simon" "->" "trev"],
     :meta {:label "mate, you missed a classic"},
     :parent "Carry on anyway"}
    {:type :conn,
     :key ["conv1" "->" "conv2"],
     :meta {:label "spot the difference?"},
     :parent "convs"}))


(deftest flattening
  (testing "dictim -> flat dictim"
    (is (= (f/flat ex) ex-flat))))


(deftest building
  (testing "flat dictim -> dictim"
    (is (= (f/build ex-flat) ex))))

