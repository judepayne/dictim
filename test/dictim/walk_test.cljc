(ns dictim.walk-test
  (:require [clojure.test :refer :all]
            [dictim.walk :refer [keywordize-keys stringify-keys]]))


(def dict1-str
  '(["Write Replica Canada" "<->" "Write Replica Australia"]
     ["Read Replica" "<-" "Master"]
     ["Write Replica" "->" "Master"]
     ["Read Replica 1" "--" "Read Replica 2"]
     ["Read Replica 1" "--" "Read Replica 2" "Kept in sync"]
     ["be" "Backend"]
     ["fe" "Frontend"]
     [:comment "This would create new shapes"]
     ["Backend" "->" "Frontend"]
     [:comment "This would define a connection over existing labels"]
     ["be" "->" "fe"]
     ["Write Replica Canada" "<->" "Write Replica Australia"]
     ["Read Replica" "<-" "Master"]
     ["x" "--" "y"]
     ["super long shape id here"
      "->"
      "super long shape id even longer here"]
     ["Database" "->" "S3" "backup"]
     ["Database" "->" "S3"]
     ["Database" "->" "S3" "backup"]
     ["High Mem Instance"
      "->"
      "EC2"
      "<-"
      "High CPU Instance"
      "Hosted By"]
     ["Stage One" "->" "Stage Two" "->" "Stage Three" "->" "Stage Four"]
     ["Stage Four" "->" "Stage One" "repeat"]
     ["a"
      "The best way to avoid responsibility is to say, \"I've got responsibilities\""]
     ["b" "Whether weary or unweary, O man, do not rest"]
     ["c"
      "I still maintain the point that designing a monolithic kernel in 1991 is a"]
     ["a"
      "->"
      "b"
      "To err is human, to moo bovine"
      {"source-arrowhead" 1,
       "target-arrowhead" {"shape" "diamond", "label" "*"}}]
     ["b"
      "<->"
      "c"
      "\"Reality is just a crutch for people who can't handle science fiction\""
      {"source-arrowhead.label" 1,
       "target-arrowhead"
       {"shape" "diamond", "style.filled" true, "label" "*"}}]
     ["d"
      "A black cat crossing your path signifies that the animal is going somewhere"]
     ["d" "->" "a" "->" "c"]))


(def dict1-keywordized
  '(["Write Replica Canada" "<->" "Write Replica Australia"]
 ["Read Replica" "<-" :Master]
 ["Write Replica" "->" :Master]
 ["Read Replica 1" "--" "Read Replica 2"]
 ["Read Replica 1" "--" "Read Replica 2" "Kept in sync"]
 [:be "Backend"]
 [:fe "Frontend"]
 [:comment "This would create new shapes"]
 [:Backend "->" :Frontend]
 [:comment "This would define a connection over existing labels"]
 [:be "->" :fe]
 ["Write Replica Canada" "<->" "Write Replica Australia"]
 ["Read Replica" "<-" :Master]
 [:x "--" :y]
 ["super long shape id here"
  "->"
  "super long shape id even longer here"]
 [:Database "->" :S3 "backup"]
 [:Database "->" :S3]
 [:Database "->" :S3 "backup"]
 ["High Mem Instance" "->" :EC2 "<-" "High CPU Instance" "Hosted By"]
 ["Stage One" "->" "Stage Two" "->" "Stage Three" "->" "Stage Four"]
 ["Stage Four" "->" "Stage One" "repeat"]
 [:a
  "The best way to avoid responsibility is to say, \"I've got responsibilities\""]
 [:b "Whether weary or unweary, O man, do not rest"]
 [:c
  "I still maintain the point that designing a monolithic kernel in 1991 is a"]
 [:a
  "->"
  :b
  "To err is human, to moo bovine"
  {:source-arrowhead 1,
   :target-arrowhead {:shape "diamond", :label "*"}}]
 [:b
  "<->"
  :c
  "\"Reality is just a crutch for people who can't handle science fiction\""
  {:source-arrowhead.label 1,
   :target-arrowhead
   {:shape "diamond", :style.filled true, :label "*"}}]
 [:d
  "A black cat crossing your path signifies that the animal is going somewhere"]
 [:d "->" :a "->" :c]))


(def dict1-re-stringified
  '(["Write Replica Canada" "<->" "Write Replica Australia"]
    ["Read Replica" "<-" "Master"]
    ["Write Replica" "->" "Master"]
    ["Read Replica 1" "--" "Read Replica 2"]
    ["Read Replica 1" "--" "Read Replica 2" "Kept in sync"]
    ["be" "Backend"]
    ["fe" "Frontend"]
    ["comment" "This would create new shapes"]
    ["Backend" "->" "Frontend"]
    ["comment" "This would define a connection over existing labels"]
    ["be" "->" "fe"]
    ["Write Replica Canada" "<->" "Write Replica Australia"]
    ["Read Replica" "<-" "Master"]
    ["x" "--" "y"]
    ["super long shape id here"
     "->"
     "super long shape id even longer here"]
    ["Database" "->" "S3" "backup"]
    ["Database" "->" "S3"]
    ["Database" "->" "S3" "backup"]
    ["High Mem Instance"
     "->"
     "EC2"
     "<-"
     "High CPU Instance"
     "Hosted By"]
    ["Stage One" "->" "Stage Two" "->" "Stage Three" "->" "Stage Four"]
    ["Stage Four" "->" "Stage One" "repeat"]
    ["a"
     "The best way to avoid responsibility is to say, \"I've got responsibilities\""]
    ["b" "Whether weary or unweary, O man, do not rest"]
    ["c"
     "I still maintain the point that designing a monolithic kernel in 1991 is a"]
    ["a"
     "->"
     "b"
     "To err is human, to moo bovine"
     {"source-arrowhead" 1,
      "target-arrowhead" {"shape" "diamond", "label" "*"}}]
    ["b"
     "<->"
     "c"
     "\"Reality is just a crutch for people who can't handle science fiction\""
     {"source-arrowhead.label" 1,
      "target-arrowhead"
      {"shape" "diamond", "style.filled" true, "label" "*"}}]
    ["d"
     "A black cat crossing your path signifies that the animal is going somewhere"]
    ["d" "->" "a" "->" "c"]))


(deftest kerwordize
  (testing "I can keywordize dictim"
    (is (= (apply keywordize-keys dict1-str) dict1-keywordized))))


(deftest stringify
  (testing "I can stringify dictim"
    (is (= (apply stringify-keys dict1-keywordized) dict1-re-stringified))))
