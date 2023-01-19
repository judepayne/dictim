(ns dictim.parse-test
  (:require [clojure.test :refer :all]
            [dictim.parse :as p]
            [instaparse.core :as insta]))

;; parsing

;; This ns is Clojure only to allow for all test d2 to be placed in
;; files in the d2 sub-directory.

(deftest shapes
  (testing "https://d2lang.com/tour/shapes"
    (is (= (p/p-d2 (slurp "test/dictim/d2/shapes.d2"))
           '([:shape [:key "imAShape"]]
            [:shape [:key "im_a_shape"]]
            [:shape [:key "im a shape"]]
            [:shape [:key "i'm a shape"]]
            [:comment [:label "notice that one-hyphen is not a connection"]]
            [:comment [:label "whereas, `a--shape` would be a connection"]]
            [:shape [:key "a-shape"]]
            [:list [:shape [:key "SQLite"]] [:shape [:key "Cassandra"]]]
            [:shape [:key "pg"] [:label "PostgreSQL"]]
            [:shape [:key "Cloud"] [:label "my cloud"]]
            [:attr [:at-key "Cloud" "." "shape"] [:label "cloud"]])))
    (is (= 1 (count (insta/parses p/p-d2 (slurp "test/dictim/d2/shapes.d2")))))))


(deftest style
  (testing "https://d2lang.com/tour/style"
    (is (= (p/p-d2 (slurp "test/dictim/d2/style.d2"))
           '([:shape
              [:key "x"]
              [:attrs
               [:attr
                [:at-key "style"]
                [:attrs
                 [:attr [:at-key "opacity"] [:label "0.6"]]
                 [:attr [:at-key "fill"] [:label "orange"]]
                 [:attr [:at-key "stroke"] [:label "\"#53C0D8\""]]
                 [:attr [:at-key "stroke-width"] [:label "5"]]
                 [:attr [:at-key "shadow"] [:label "true"]]]]]]
             [:shape
              [:key "y"]
              [:attrs
               [:attr
                [:at-key "style"]
                [:attrs
                 [:attr [:at-key "opacity"] [:label "0.6"]]
                 [:attr [:at-key "fill"] [:label "red"]]
                 [:attr [:at-key "3d"] [:label "true"]]
                 [:attr [:at-key "stroke"] [:label "black"]]]]]]
             [:conn
              [:ekey [:ekey-part-last "x "]]
              [:dir "->"]
              [:key "y"]
              [:attrs
               [:attr
                [:at-key "style"]
                [:attrs
                 [:attr [:at-key "stroke"] [:label "green"]]
                 [:attr [:at-key "opacity"] [:label "0.5"]]
                 [:attr [:at-key "stroke-width"] [:label "2"]]
                 [:attr [:at-key "stroke-dash"] [:label "5"]]]]]]
             [:comment [:label "Float between 0 and 1"]]
             [:attr [:at-key "opacity"] [:label "0.6"]]
             [:comment [:label "CSS color or hex code"]]
             [:attr [:at-key "fill"] [:label "orange"]]
             [:comment [:label "CSS color or hex code"]]
             [:attr [:at-key "stroke"] [:label "\"#53C0D8\""]]
             [:comment [:label "Integer betwen 1 and 15"]]
             [:attr [:at-key "stroke-width"] [:label "5"]]
             [:comment [:label "Integer betwen 0 and 10"]]
             [:attr [:at-key "stroke-dash"] [:label "5"]]
             [:comment [:label "Only applicable to shapes, except ovals"]]
             [:comment [:label "Integer betwen 0 and 20"]]
             [:attr [:at-key "border-radius"] [:label "4"]]
             [:comment [:label "CSS color or hex code"]]
             [:attr [:at-key "font-color"] [:label "red"]]
             [:comment [:label "Only applicable to shapes"]]
             [:comment [:label "true or false"]]
             [:attr [:at-key "shadow"] [:label "true"]]
             [:comment [:label "Only applicable to shapes"]]
             [:comment [:label "true or false"]]
             [:attr [:at-key "multiple"] [:label "true"]]
             [:comment [:label "Only applicable to squares"]]
             [:comment [:label "true or false"]]
             [:attr [:at-key "3d"] [:label "true"]]
             [:comment [:label "Only applicable to edges"]]
             [:attr [:at-key "animated"] [:label "false"]]
             [:comment [:label "Only applicable to shapes"]]
             [:comment [:label "Can be an external URL"]]
             [:attr [:at-key "link"] [:label "https://google.com"]]
             [:comment [:label "Or an internal board"]]
             [:attr [:at-key "link"] [:label "Overview.Untitled Board 2"]])))
    (is (= 4 (count (insta/parses p/p-d2 (slurp "test/dictim/d2/style.d2")))))))


(deftest containers
  (testing "https://d2lang.com/tour/containers"
    (is (= (p/p-d2 (slurp "test/dictim/d2/containers.d2"))
           '([:shape [:key "server"]]
             [:comment [:label "Declares a shape inside of another shape"]]
             [:shape [:key "server" "." "process"]]
             [:comment
              [:label "Can declare the container and child in same line"]]
             [:shape [:key "im a parent" "." "im a child"]]
             [:comment
              [:label "Since connections can also declare keys, this works too"]]
             [:conn
              [:ekey
               [:ekey-part "apartment"]
               "."
               [:ekey-part "Bedroom"]
               "."
               [:ekey-part-last "Bathroom "]]
              [:dir "->"]
              [:key "office" "." "Spare Room" "." "Bathroom"]
              [:label "Portal"]]
             [:ctr
              [:key "clouds"]
              [:ctr
               [:key "aws"]
               [:conn
                [:ekey [:ekey-part-last "load_balancer "]]
                [:dir "->"]
                [:key "api"]]
               [:conn [:ekey [:ekey-part-last "api "]] [:dir "->"] [:key "db"]]]
              [:ctr
               [:key "gcloud"]
               [:conn [:ekey [:ekey-part-last "auth "]] [:dir "->"] [:key "db"]]]
              [:conn
               [:ekey [:ekey-part-last "gcloud "]]
               [:dir "->"]
               [:key "aws"]]]
             [:ctr
              [:key "clouds"]
              [:ctr
               [:key "aws"]
               [:label "AWS "]
               [:conn
                [:ekey [:ekey-part-last "load_balancer "]]
                [:dir "->"]
                [:key "api"]]
               [:conn [:ekey [:ekey-part-last "api "]] [:dir "->"] [:key "db"]]]
              [:ctr
               [:key "gcloud"]
               [:label "Google Cloud "]
               [:conn [:ekey [:ekey-part-last "auth "]] [:dir "->"] [:key "db"]]]
              [:conn
               [:ekey [:ekey-part-last "gcloud "]]
               [:dir "->"]
               [:key "aws"]]]
             [:conn
              [:ekey [:ekey-part-last "users "]]
              [:dir "->"]
              [:key "clouds" "." "aws" "." "load_balancer"]]
             [:conn
              [:ekey [:ekey-part-last "users "]]
              [:dir "->"]
              [:key "clouds" "." "gcloud" "." "auth"]]
             [:conn
              [:ekey [:ekey-part "ci"] "." [:ekey-part-last "deploys "]]
              [:dir "->"]
              [:key "clouds"]]
             [:ctr [:key "christmas"] [:shape [:key "presents"]]]
             [:ctr
              [:key "birthdays"]
              [:shape [:key "presents"]]
              [:conn
               [:ekey
                [:ekey-part "_"]
                "."
                [:ekey-part "christmas"]
                "."
                [:ekey-part-last "presents "]]
               [:dir "->"]
               [:key "presents"]
               [:label "regift"]]
              [:attr
               [:at-key "_" "." "christmas" "." "style" "." "fill"]
               [:label "\"#ACE1AF\""]]])))
    (is (= 1 (count (insta/parses p/p-d2 (slurp "test/dictim/d2/containers.d2")))))))


(deftest connections
  (testing "https://d2lang.com/tour/connections"
    (is (= (p/p-d2 (slurp "test/dictim/d2/connections.d2"))
           '([:conn
              [:ekey [:ekey-part-last "Write Replica Canada "]]
              [:dir "<->"]
              [:key "Write Replica Australia"]]
             [:conn
              [:ekey [:ekey-part-last "Read Replica "]]
              [:dir "<-"]
              [:key "Master"]]
             [:conn
              [:ekey [:ekey-part-last "Write Replica "]]
              [:dir "->"]
              [:key "Master"]]
             [:conn
              [:ekey [:ekey-part-last "Read Replica 1 "]]
              [:dir "--"]
              [:key "Read Replica 2"]]
             [:conn
              [:ekey [:ekey-part-last "Read Replica 1 "]]
              [:dir "--"]
              [:key "Read Replica 2"]
              [:label "Kept in sync"]]
             [:shape [:key "be"] [:label "Backend"]]
             [:shape [:key "fe"] [:label "Frontend"]]
             [:comment [:label "This would create new shapes"]]
             [:conn
              [:ekey [:ekey-part-last "Backend "]]
              [:dir "->"]
              [:key "Frontend"]]
             [:comment
              [:label "This would define a connection over existing labels"]]
             [:conn [:ekey [:ekey-part-last "be "]] [:dir "->"] [:key "fe"]]
             [:conn
              [:ekey [:ekey-part-last "Write Replica Canada "]]
              [:dir "<->"]
              [:key "Write Replica Australia"]]
             [:conn
              [:ekey [:ekey-part-last "Read Replica "]]
              [:dir "<-"]
              [:key "Master"]]
             [:conn [:ekey [:ekey-part-last "x "]] [:dir "--"] [:key "y"]]
             [:conn
              [:ekey [:ekey-part-last "super long shape id here "]]
              [:dir "->"]
              [:key "super long shape id even longer here"]]
             [:conn
              [:ekey [:ekey-part-last "Database "]]
              [:dir "->"]
              [:key "S3"]
              [:label "backup"]]
             [:conn
              [:ekey [:ekey-part-last "Database "]]
              [:dir "->"]
              [:key "S3"]]
             [:conn
              [:ekey [:ekey-part-last "Database "]]
              [:dir "->"]
              [:key "S3"]
              [:label "backup"]]
             [:conn
              [:ekey [:ekey-part-last "High Mem Instance "]]
              [:dir "->"]
              [:ekey [:ekey-part-last "EC2 "]]
              [:dir "<-"]
              [:key "High CPU Instance"]
              [:label "Hosted By"]]
             [:conn
              [:ekey [:ekey-part-last "Stage One "]]
              [:dir "->"]
              [:ekey [:ekey-part-last "Stage Two "]]
              [:dir "->"]
              [:ekey [:ekey-part-last "Stage Three "]]
              [:dir "->"]
              [:key "Stage Four"]]
             [:conn
              [:ekey [:ekey-part-last "Stage Four "]]
              [:dir "->"]
              [:key "Stage One"]
              [:label "repeat"]]
             [:shape
              [:key "a"]
              [:label
               "The best way to avoid responsibility is to say, \"I've got responsibilities\""]]
             [:shape
              [:key "b"]
              [:label "Whether weary or unweary, O man, do not rest"]]
             [:shape
              [:key "c"]
              [:label
               "I still maintain the point that designing a monolithic kernel in 1991 is a"]]
             [:conn
              [:ekey [:ekey-part-last "a "]]
              [:dir "->"]
              [:key "b"]
              [:label "To err is human, to moo bovine "]
              [:attrs
               [:attr [:at-key "source-arrowhead"] [:label "1"]]
               [:attr
                [:at-key "target-arrowhead"]
                [:attr-label [:label "* "]]
                [:attrs [:attr [:at-key "shape"] [:label "diamond"]]]]]]
             [:conn
              [:ekey [:ekey-part-last "b "]]
              [:dir "<->"]
              [:key "c"]
              [:label
               "\"Reality is just a crutch for people who can't handle science fiction\" "]
              [:attrs
               [:attr [:at-key "source-arrowhead" "." "label"] [:label "1"]]
               [:attr
                [:at-key "target-arrowhead"]
                [:attr-label [:label "* "]]
                [:attrs
                 [:attr [:at-key "shape"] [:label "diamond"]]
                 [:attr [:at-key "style" "." "filled"] [:label "true"]]]]]]
             [:shape
              [:key "d"]
              [:label
               "A black cat crossing your path signifies that the animal is going somewhere"]]
             [:conn
              [:ekey [:ekey-part-last "d "]]
              [:dir "->"]
              [:ekey [:ekey-part-last "a "]]
              [:dir "->"]
              [:key "c"]])))
    (is (= 1 (count (insta/parses p/p-d2 (slurp "test/dictim/d2/connections.d2")))))))


(deftest lineendings
  (testing "Can parse various messy line endings"
    (is (= (p/p-d2 (slurp "test/dictim/d2/lineendings.d2"))
           '([:list
              [:shape [:key "a"]]
              [:shape [:key "b"]]
              [:shape [:key "c"]]
              [:shape [:key "d"]]]
             [:shape [:key "hello"]]
             [:list
              [:shape [:key "e"]]
              [:shape [:key "f"]]
              [:shape [:key "g"]]
              [:shape [:key "h"]]]
             [:shape [:key "goodbye"]])))
    (is (= 1 (count (insta/parses p/p-d2 (slurp "test/dictim/d2/lineendings.d2")))))))



