(ns dictim.json-test
  (:require [clojure.test :refer :all]
            [dictim.json :refer [to-json from-json]]))


(def dictim-raw
  ['(["imAShape"]	    
     ["im_a_shape"]
     ["im a shape"]
     ["i'm a shape"]
     [:comment "notice that one-hyphen is not a connection"]
     [:comment "whereas, `a--shape` would be a connection"]
     ["a-shape"]
     [:list ["SQLite"] ["Cassandra"]]
     ["pg" "PostgreSQL"]
     ["Cloud" "my cloud"]
     ["Cloud" {"shape" "cloud"}])

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
      ["_.christmas" {"style.fill" "\"#ACE1AF\""}]])


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
     ["d" "->" "a" "->" "c"])

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
      ["AWS S3 Vancouver" "->" "disks"]])

   ["2007" "->" "2012" "Five\\nyears\\nlater"]

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
     ["results" "->" "favorable" "->" "will of the people"])

   '(["bravo team" {"shape" "person"}]	    
     ["charlie team" {"shape" "person"}]
     ["command center" {"shape" "cloud"}]
     ["hq" {"shape" "rectangle"}]
     {"*" {"&shape" "person", "style.multiple" true}})

   '({"vars"
      {"primaryColors"
       {"button" {"active" "\"#4baae5\"", "border" "black"}}}}
     ["button"
      {"width" 100,
       "height" 40,
       "style"
       {"border-radius" 5,
        "fill" "${primaryColors.button.active}",
        "stroke" "${primaryColors.button.border}"}}])

   '(["lady 1"]
     ["lady 2"]
     ["barbie"]
     ["lady 1" "->" "barbie" "hi barbie"]
     ["lady 2" "->" "barbie" "hi barbie"]
     ["lady*" "->" "barbie" ["*"] {"style.stroke" "pink"}])

   '(["pig" nil ["piglet"]])

   '(["test" {"direction" [:list "a" "...${ab}"]}])
   ])


(defn convert-keywords
  [data]
  (clojure.walk/postwalk (fn [x] (if (keyword? x) (name x) x)) data))


(def dictim (convert-keywords dictim-raw))


(deftest round-trip
  (testing "I can round-trip dictim to json and back"
    (mapv
     (fn [dict]
       (is (= dict
              (-> dict to-json from-json))))
     dictim)))
