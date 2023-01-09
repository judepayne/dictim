(ns dictim.core-test
  (:require [clojure.test :refer :all]
            [dictim.compile :as c]
            [dictim.parse :as p]
            [dictim.flat :as fl]))

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


(def ex-graph-d2 "jude: Friends {\n  tristram: T Biggs expanded {\n    tris: TAB\n    maddie: Madeline\n    tris -- maddie: wedding bells?\n    tris -> children: previously sired\n    children: the brood {\n      Oliver: Eldest {\n        style: {\n          fill: orange\n        }\n      }\n      Roof: Good footballer {\n        shape: person\n      }\n    }\n  }\n}")


(def ex-sequence
  [:convs "Office Conversations"
   [:conv1 "Office conversation 1"
    {:shape "sequence_diagram"}
    [:list :alice :bob]
    [:alice "Alice" {:shape "person" :style {:fill "orange"}}]
    [:bob "Bobby"]
    ["awkward small talk"
     [:alice "->" :bob "um, hi"]
     [:bob "->" :alice "oh, hello"]
     ["icebreaker attempt"
      [:alice "->" :bob "what did you have for lunch?"]]
     [:fail {:style {:fill "green"}}
      [:bob "->" :alice "that's personal"]]]]
   [:conv2 "Office conversation 2"
    {:shape "sequence_diagram" :order '(:simon :trev)}
    [:simon "Simon" {:shape "person"}]
    [:trev "Trevor"]
    ["failed conversation"
     [:simon "->" :trev "seen the football"]
     [:trev"->" :simon "no, I was at my gran's"]
     ["Carry on anyway"
      [:simon "->" :trev "mate, you missed a classic"]]]]
   [:conv1 "->" :conv2 "spot the difference?"]])


(def ex-sequence-d2 "convs: Office Conversations {\n  conv1: Office conversation 1 {\n    shape: sequence_diagram\n    bob\n    alice\n    alice: Alice {\n      shape: person\n      style: {\n        fill: orange\n      }\n    }\n    bob: Bobby\n    awkward small talk: {\n      alice -> bob: um, hi\n      bob -> alice: oh, hello\n      icebreaker attempt: {\n        alice -> bob: what did you have for lunch?\n      }\n      fail: {\n        style: {\n          fill: green\n        }\n        bob -> alice: that's personal\n      }\n    }\n  }\n  conv2: Office conversation 2 {\n    shape: sequence_diagram\n    simon\n    trev\n    simon: Simon {\n      shape: person\n    }\n    trev: Trevor\n    failed conversation: {\n      simon -> trev: seen the football\n      trev -> simon: no, I was at my gran's\n      Carry on anyway: {\n        simon -> trev: mate, you missed a classic\n      }\n    }\n  }\n  conv1 -> conv2: spot the difference?\n}")

(deftest compilation
  (testing "Compiling dictim to d2. no.1"
    (is (= (c/d2 ex-graph) ex-graph-d2)))
  (testing "Compiling dictim to d2. no.2"
    (is (= (c/d2 ex-sequence) ex-sequence-d2))))


(def ex-class
  [:MyClass { :shape "class" :field "\"[]string\"" :-reader "io.RuneReader"}])



