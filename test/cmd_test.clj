#!/usr/bin/env bb

(ns user)

;; test script to be run in .circleci environment
;; assume: bb, bbin & dictim installed
(require '[babashka.cli :as cli])

(def cli-options {:cmd {:default :bin}})


(def dictim-cmd
  (get
   {:bin "./bin/dictim"
    :win "bin\dictim.exe"
    :bbin "dictim"
    :bb "bb dictim.jar"}
   (:cmd (cli/parse-opts *command-line-args* {:spec cli-options}))))


(require '[babashka.process :refer [shell]])
(require '[clojure.edn :refer [read-string]])

;; test that dictim is installed
(assert (:out (shell {:out :string} dictim-cmd "-v")))


(def dict [:plan "build the resource"])
(def d2 "plan: build the resource\n")

;; test compilation
(assert (= (:out (shell {:out :string :in (pr-str dict)} dictim-cmd "-c"))
           d2))


;; test parsing
(assert (= (read-string (:out (shell {:out :string :in d2} dictim-cmd "-k" "-p")))
           (list dict)))


(def d2-2 "direction: up
1STR {
  db: sql server\n1.5TB {shape: cylinder}
  middle_tier: java\nspring boot {style.border-radius: 8}
  queue: kafka {shape: queue}
  processor: C++ grid {style.multiple: true}
  gui: react\ngui
  db <-> middle_tier
  middle_tier <-> gui
  middle_tier <-> queue
  queue <-> processor
}
ABC1 -> 1STR: client ref data
ABC1 -> 1STR: instrument ref data
ABC2 -> 1STR: equities trade data
ABC3 -> 1STR: fx trades data
ABC4 -> 1STR: rates trade data
1STR -> XYZ1: MIFID reg reports
1STR -> XYZ2: Other reg reports")


(def dict-json "[{\"direction\":\"up\"},[\"1STR\",[\"db\",\"sql server\"],[\"1.5TB\",{\"shape\":\"cylinder\"}],[\"middle_tier\",\"java\"],[\"spring boot\",{\"style.border-radius\":8}],[\"queue\",\"kafka\",{\"shape\":\"queue\"}],[\"processor\",\"C++ grid\",{\"style.multiple\":true}],[\"gui\",\"react\"],[\"gui\"],[\"db\",\"<->\",\"middle_tier\"],[\"middle_tier\",\"<->\",\"gui\"],[\"middle_tier\",\"<->\",\"queue\"],[\"queue\",\"<->\",\"processor\"]],[\"ABC1\",\"->\",\"1STR\",\"client ref data\"],[\"ABC1\",\"->\",\"1STR\",\"instrument ref data\"],[\"ABC2\",\"->\",\"1STR\",\"equities trade data\"],[\"ABC3\",\"->\",\"1STR\",\"fx trades data\"],[\"ABC4\",\"->\",\"1STR\",\"rates trade data\"],[\"1STR\",\"->\",\"XYZ1\",\"MIFID reg reports\"],[\"1STR\",\"->\",\"XYZ2\",\"Other reg reports\"]]\n")



(def dict-json-pretty
"[\n  {\n    \"direction\":\"up\"\n  },\n  [\n    \"1STR\",\n    [\n      \"db\",\n      \"sql server\"\n    ],\n    [\n      \"1.5TB\",\n      {\n        \"shape\":\"cylinder\"\n      }\n    ],\n    [\n      \"middle_tier\",\n      \"java\"\n    ],\n    [\n      \"spring boot\",\n      {\n        \"style.border-radius\":8\n      }\n    ],\n    [\n      \"queue\",\n      \"kafka\",\n      {\n        \"shape\":\"queue\"\n      }\n    ],\n    [\n      \"processor\",\n      \"C++ grid\",\n      {\n        \"style.multiple\":true\n      }\n    ],\n    [\n      \"gui\",\n      \"react\"\n    ],\n    [\n      \"gui\"\n    ],\n    [\n      \"db\",\n      \"<->\",\n      \"middle_tier\"\n    ],\n    [\n      \"middle_tier\",\n      \"<->\",\n      \"gui\"\n    ],\n    [\n      \"middle_tier\",\n      \"<->\",\n      \"queue\"\n    ],\n    [\n      \"queue\",\n      \"<->\",\n      \"processor\"\n    ]\n  ],\n  [\n    \"ABC1\",\n    \"->\",\n    \"1STR\",\n    \"client ref data\"\n  ],\n  [\n    \"ABC1\",\n    \"->\",\n    \"1STR\",\n    \"instrument ref data\"\n  ],\n  [\n    \"ABC2\",\n    \"->\",\n    \"1STR\",\n    \"equities trade data\"\n  ],\n  [\n    \"ABC3\",\n    \"->\",\n    \"1STR\",\n    \"fx trades data\"\n  ],\n  [\n    \"ABC4\",\n    \"->\",\n    \"1STR\",\n    \"rates trade data\"\n  ],\n  [\n    \"1STR\",\n    \"->\",\n    \"XYZ1\",\n    \"MIFID reg reports\"\n  ],\n  [\n    \"1STR\",\n    \"->\",\n    \"XYZ2\",\n    \"Other reg reports\"\n  ]\n]\n")


(assert (= (:out (shell {:out :string :in d2-2} dictim-cmd "-j" "-p"))
           dict-json))


(assert (= (:out (shell {:out :string :in d2-2} dictim-cmd "-j" "-b" "-p"))
            dict-json-pretty))



(def d2
  "direction: up\n1STR:   {\n  db: sql server\n  1.5TB:  {\n    shape: cylinder\n  }\n  middle_tier: java\n  spring boot:  {\n    style.border-radius: 8\n  }\n  queue: kafka  {\n    shape: queue\n  }\n  processor: C++ grid  {\n    style.multiple: true\n  }\n  gui: react\n  gui\n  db <-> middle_tier\n  middle_tier <-> gui\n  middle_tier <-> queue\n  queue <-> processor\n}\nABC1 -> 1STR: client ref data\nABC1 -> 1STR: instrument ref data\nABC2 -> 1STR: equities trade data\nABC3 -> 1STR: fx trades data\nABC4 -> 1STR: rates trade data\n1STR -> XYZ1: MIFID reg reports\n1STR -> XYZ2: Other reg reports\n")


(assert (= (:out (shell {:out :string :in dict-json} dictim-cmd "-c"))
           d2))
