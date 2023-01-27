(ns dictim.parse-test
  (:require [clojure.test :refer :all]
            [dictim.parse :as p]
            [dictim.validate :as v]
            [instaparse.core :as insta]))

;; parsing

;; This ns is Clojure only to allow for all test d2 to be placed in
;; files in the d2 sub-directory.

(def num-parses #'p/num-parses)


(deftest shapes
  (testing "Shapes"
    (let [d2 (slurp "test/dictim/d2/shapes.d2")
          dict (p/dictim d2)]
      (is (= 1 (num-parses d2)))
      (is (= true (v/all-valid? dict)))
      (is (= dict
             '(["imAShape"]
               ["im_a_shape"]
               ["im a shape"]
               ["i'm a shape"]
               [:comment "notice that one-hyphen is not a connection"]
               [:comment "whereas, `a--shape` would be a connection"]
               ["a-shape"]
               [:list ["SQLite"] ["Cassandra"]]
               ["pg" "PostgreSQL"]
               ["Cloud" "my cloud"]
               {"Cloud.shape" "cloud"}))))))


(deftest containers
  (testing "Containers"
    (let [d2 (slurp "test/dictim/d2/containers.d2")
          dict (p/dictim d2)]
      (is (= 1 (num-parses d2)))
      (is (= true (v/all-valid? dict)))
      (is (= dict
             '(["server"]
               [:comment "Declares a shape inside of another shape"]
               ["server.process"]
               [:comment "Can declare the container and child in same line"]
               ["im a parent.im a child"]
               [:comment "Since connections can also declare keys, this works too"]
               ["apartment.Bedroom.Bathroom"
                "->"
                "office.Spare Room.Bathroom"
                "Portal"]
               ["clouds"
                ["aws" ["load_balancer" "->" "api"] ["api" "->" "db"]]
                ["gcloud" ["auth" "->" "db"]]
                ["gcloud" "->" "aws"]]
               ["clouds"
                ["aws" "AWS" ["load_balancer" "->" "api"] ["api" "->" "db"]]
                ["gcloud" "Google Cloud" ["auth" "->" "db"]]
                ["gcloud" "->" "aws"]]
               ["users" "->" "clouds.aws.load_balancer"]
               ["users" "->" "clouds.gcloud.auth"]
               ["ci.deploys" "->" "clouds"]
               ["christmas" ["presents"]]
               ["birthdays"
                ["presents"]
                ["_.christmas.presents" "->" "presents" "regift"]
                {"_.christmas.style.fill" "\"#ACE1AF\""}]))))))


(deftest connections
  (testing "Connections"
    (let [d2 (slurp "test/dictim/d2/connections.d2")
          dict (p/dictim d2)]
      (is (= 1 (num-parses d2)))
      (is (= true (v/all-valid? dict)))
      (is (= dict
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
               ["d" "->" "a" "->" "c"]))))))


(deftest styles
  (testing "Styles"
    (let [d2 (slurp "test/dictim/d2/style.d2")
          dict (p/dictim d2)]
      (is (= 1 (num-parses d2)))
      (is (= true (v/all-valid? dict))))))


(deftest classes
  (testing "Classes"
    (let [d2 (slurp "test/dictim/d2/classes.d2")
          dict (p/dictim d2)]
      (is (= 1 (num-parses d2)))
      (is (= true (v/all-valid? dict)))
      (is (= dict
             '(["MyClass"
                {"shape" "class"}
                ["field" "\"[]string\""]
                ["method(a uint64)" "(x, y int)"]]
               ["D2 Parser"
                {"shape" "class"}
                [:comment "Default visibility is + so no need to specify."]
                ["+reader" "io.RuneReader"]
                ["readerPos" "d2ast.Position"]
                [:comment "Private field."]
                ["-lookahead" "\"[]rune\""]
                [:comment "Protected field."]
                [:comment
                 "We have to escape the # to prevent the line from being parsed as a comment."]
                ["\\#lookaheadPos" "d2ast.Position"]
                ["+peek()" "(r rune, eof bool)"]
                ["rewind()"]
                ["commit()"]
                ["\\#peekn(n int)" "(s string, eof bool)"]]
               ["\"github.com/terrastruct/d2parser.git\"" "--" "D2 Parser"]))))))


(deftest sql
  (testing "Sql Tables"
    (let [d2 (slurp "test/dictim/d2/sql.d2")
          dict (p/dictim d2)]
      (is (= 1 (num-parses d2)))
      (is (= true (v/all-valid? dict)))
      (is (= dict
             '(["my_table"
                {"shape" "sql_table"}
                ["id" "int" {"constraint" "primary_key"}]
                ["last_updated" "timestamp with time zone"]]
               ["objects"
                {"shape" "sql_table"}
                ["id" "int" {"constraint" "primary_key"}]
                ["disk" "int" {"constraint" "foreign_key"}]
                ["json" "jsonb" {"constraint" "unique"}]
                ["last_updated" "timestamp with time zone"]]
               ["disks"
                {"shape" "sql_table"}
                ["id" "int" {"constraint" "primary_key"}]]
               ["objects.disk" "->" "disks.id"]
               ["cloud"
                ["disks"
                 {"shape" "sql_table"}
                 ["id" "int" {"constraint" "primary_key"}]]
                ["blocks"
                 {"shape" "sql_table"}
                 ["id" "int" {"constraint" "primary_key"}]
                 ["disk" "int" {"constraint" "foreign_key"}]
                 ["blob" "blob"]]
                ["blocks.disk" "->" "disks.id"]
                ["AWS S3 Vancouver" "->" "disks"]]))))))


(deftest sequence-digrams
  (testing "Sequence Diagrams"
    (let [d2 (slurp "test/dictim/d2/sequence.d2")
          dict (p/dictim d2)]
      (is (= 1 (num-parses d2)))
      (is (= true (v/all-valid? dict)))
      (is (= dict
             '({"direction" "right"}
               ["Before and after becoming friends"
                ["2007"
                 "Office chatter in 2007"
                 {"shape" "sequence_diagram"}
                 ["alice" "Alice"]
                 ["bob" "Bobby"]
                 ["awkward small talk"
                  ["alice" "->" "bob" "uhm, hi"]
                  ["bob" "->" "alice" "oh, hello"]
                  ["icebreaker attempt"
                   ["alice" "->" "bob" "what did you have for lunch?"]]
                  ["unfortunate outcome" ["bob" "->" "alice" "that's personal"]]]]
                ["2012"
                 "Office chatter in 2012"
                 {"shape" "sequence_diagram"}
                 ["alice" "Alice"]
                 ["bob" "Bobby"]
                 ["alice" "->" "bob" "Want to play with ChatGPT?"]
                 ["bob" "->" "alice" "Yes!"]
                 ["bob" "->" "alice.play" "Write a play..."]
                 ["alice.play" "->" "bob.play" "about 2 friends..."]
                 ["bob.play" "->" "alice.play" "who find love..."]
                 ["alice.play" "->" "bob.play" "in a sequence diagram"]]
                ["2007" "->" "2012" "Five\\nyears\\nlater"]]
               ["Office chatter"
                {"shape" "sequence_diagram"}
                ["alice" "Alice"]
                ["bob" "Bobby"]
                ["awkward small talk"
                 ["alice" "->" "bob" "uhm, hi"]
                 ["bob" "->" "alice" "oh, hello"]
                 ["icebreaker attempt"
                  ["alice" "->" "bob" "what did you have for lunch?"]]
                 ["unfortunate outcome" ["bob" "->" "alice" "that's personal"]]]]
               {"shape" "sequence_diagram"}
               ["alice" "->" "bob" "What does it mean\\nto be well-adjusted?"]
               ["bob"
                "->"
                "alice"
                "The ability to play bridge or\\ngolf as if they were games."]
               {"shape" "sequence_diagram"}
               [:comment
                "Remember that semicolons allow multiple objects to be defined in one line"]
               [:comment "Actors will appear from left-to-right as a, b, c, d..."]
               [:list ["a"] ["b"] ["c"] ["d"]]
               [:comment "... even if the connections are in a different order"]
               ["c" "->" "d"]
               ["d" "->" "a"]
               ["b" "->" "d"]))))))


(deftest interactive
  (testing "Interactive features"
    (let [d2 (slurp "test/dictim/d2/interactive.d2")
          dict (p/dictim d2)]
      (is (= 1 (num-parses d2)))
      (is (= true (v/all-valid? dict)))
      (is (= dict
             '(["x"
                {"tooltip" "Total abstinence is easier than perfect moderation "}]
               ["y"
                {"tooltip"
                 "Gee, I feel kind of LIGHT in the head now,\\nknowing I can't make my satellite dish PAYMENTS! "}]
               ["x" "->" "y"]
               ["x" "I'm a Mac" {"link" "https://apple.com"}]
               ["y" "And I'm a PC" {"link" "https://microsoft.com"}]
               ["x" "->" "y" "gazoontit"]))))))


(deftest textandcode
  (testing "Text and Code"
    (let [d2 (slurp "test/dictim/d2/textandcode.d2")
          dict (p/dictim d2)]
      (is (= 1 (num-parses d2)))
      (is (= true (v/all-valid? dict)))
      (is (= dict
             '(["explanation"
                "|md\n  # I can do headers\n  - lists\n  - lists\n\n  And other normal markdown stuff\n|"]
               ["my_code"
                "|||ts\n  declare function getSmallPet(): Fish | Bird;\n  const works = (a > 1) || (b < 2)\n|||"]
               ["my_code"
                "|`ts\n  declare function getSmallPet(): Fish | Bird;\n  const works = (a > 1) || (b < 2)\n`|"]
               ["amscd plugin"
                ["ex"
                 "|tex\n\\\\begin{CD} B @>{\\\\text{very long label}}>> C S^{{\\\\mathcal{W}}_\\\\Lambda}\\\\otimes T @>j>> T\\\\\\\\ @VVV V \\\\end{CD}\n|"]]
               ["multilines"
                ["ex"
                 "|tex\n\\\\displaylines{x = a + b \\\\\\\\ y = b + c}\n\\\\sum_{k=1}^{n} h_{k} \\\\int_{0}^{1} \\\\bigl(\\\\partial_{k} f(x_{k-1}+t h_{k} e_{k}) -\\\\partial_{k} f(a)\\\\bigr) \\\\,dt\n|"]]
               ["title"
                "A winning strategy"
                {"shape" "text",
                 "near" "top-center",
                 "style" {"font-size" 55, "italic" true}}]
               ["poll the people" "->" "results"]
               ["results" "->" "unfavorable" "->" "poll the people"]
               ["results" "->" "favorable" "->" "will of the people"]))))))
