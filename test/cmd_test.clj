#!/usr/bin/env bb

(ns user)

;; test script to be run in .circleci environment
;; assume: bb, bbin & dictim installed
(require '[babashka.cli :as cli])


(def cli-options {:cmd {:default :bin}})


(def env (:cmd (cli/parse-opts *command-line-args* {:spec cli-options})))


(def dictim-cmd
  (get
   {:bin "./bin/dict"
    :win "bin\\dict.exe"
    :bbin "dict"
    :bb "bb dict.jar"}
   env))



;; diagnostic function
#_(defn compare-strings [s1 s2]
    (let [z (partition 2 (interleave (seq s1) (seq s2)))
          cmp (reduce
               (fn [acc [c1 c2]]
                 (if (= c1 c2)
                   (update-in acc [:idx] inc)
                   (reduced (-> acc
                                (update-in [:idx] inc)
                                (assoc-in [:match?] false)
                                (assoc-in [:char1] c1)
                                (assoc-in [:char2] c2)))))
               {:idx -1
                :match? true}
               z)]
      (if (:match? cmp)
        (println cmp)
        (println (str "break at index " (:idx cmp) "\n"
                      "char 1: " (:char1 cmp) " (" (int (:char1 cmp))  ") vs. char2: "
                      (:char2 cmp) " (" (int (:char2 cmp))  ")\n"
                      (subs s1 0 (:idx cmp)))))))


;; we need to compare right trimmed output since the cmd line tool uses
;; println for clean, human readable output. println adds \n in mac/linux
;; but \r\n in windows.
(defn trim= [s1 s2]
  (if (re-find #"(?i)windows" (System/getProperty "os.name"))
    ;; Windows: normalize CRLF to LF before comparison
    (= (-> s1 str/trimr (str/replace #"\r\n" "\n"))
       (-> s2 str/trimr (str/replace #"\r\n" "\n")))
    ;; Unix systems: simple trim comparison
    (= (str/trimr s1) (str/trimr s2))))


(require '[babashka.process :refer [shell]])
(require '[clojure.edn :refer [read-string]])
(require '[clojure.string :as str])

;; test that dictim is installed
(assert (:out (shell {:out :string} dictim-cmd "-v")))


(def dict [:plan "build the resource"])
(def d2 "plan: build the resource\n")

;; test compilation
(assert (trim= (:out (shell {:out :string :in (pr-str dict)} dictim-cmd "-c"))
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


(assert (trim= (:out (shell {:out :string :in d2-2} dictim-cmd "-j" "-p"))
               dict-json))


(assert (trim= (:out (shell {:out :string :in d2-2} dictim-cmd "-j" "-m" "-p"))
               dict-json-pretty))


(def d2-3
   "direction: up\n1STR: {\n  db: sql server\n  1.5TB: {shape: cylinder}\n  middle_tier: java\n  spring boot: {style.border-radius: 8}\n  queue: kafka {shape: queue}\n  processor: C++ grid {style.multiple: true}\n  gui: react\n  gui\n  db <-> middle_tier\n  middle_tier <-> gui\n  middle_tier <-> queue\n  queue <-> processor\n}\nABC1 -> 1STR: client ref data\nABC1 -> 1STR: instrument ref data\nABC2 -> 1STR: equities trade data\nABC3 -> 1STR: fx trades data\nABC4 -> 1STR: rates trade data\n1STR -> XYZ1: MIFID reg reports\n1STR -> XYZ2: Other reg reports\n")


(assert (trim= (:out (shell {:out :string :in dict-json} dictim-cmd "-c"))
               d2-3))


(def dict2 "([\"John\" \"Manager\"] [\"Pauli\" \"Developer\"] [\"John\" \"->\" \"Pauli\" \"I wish I still had your job\"])")


(def dict2-flat
"({:type :shape, :key \"John\", :meta {:label \"Manager\"}}\n {:type :shape, :key \"Pauli\", :meta {:label \"Developer\"}}\n {:type :conn,\n  :key [\"John\" \"->\" \"Pauli\"],\n  :meta {:label \"I wish I still had your job\"}})\n")


(def dict2-built "([\"John\" \"Manager\"]\n [\"Pauli\" \"Developer\"]\n [\"John\" \"->\" \"Pauli\" \"I wish I still had your job\"])\n")


(assert (trim= (:out (shell {:out :string :in dict2} dictim-cmd "-f"))
               dict2-flat))


(assert (trim= (:out (shell {:out :string :in dict2-flat} dictim-cmd "-b"))
               dict2-built))


;; ============================================================================
;; FILE-BASED TESTS
;; ============================================================================

(require '[clojure.java.io :as io])

(defn test-with-files [test-name input-file command expected-file]
  (let [input-path (str "test/cmd/inputs/" input-file)
        output-path (str "test/cmd/outputs/" test-name "-output")
        expected-path (str "test/cmd/expected_outputs/" expected-file)
        cmd-parts (str/split command #" ")
        result (apply shell {:out :string :in (slurp input-path)} 
                      dictim-cmd cmd-parts)
        output (:out result)]
    ;; Ensure outputs directory exists
    (.mkdirs (io/file "test/cmd/outputs"))
    (spit output-path output)
    (assert (trim= output (slurp expected-path))
            (str "Test failed: " test-name))))

(defn test-with-files-and-template [test-name input-file template-file command expected-file]
  (let [input-path (str "test/cmd/inputs/" input-file)
        template-path (str "test/cmd/inputs/" template-file)
        output-path (str "test/cmd/outputs/" test-name "-output")
        expected-path (str "test/cmd/expected_outputs/" expected-file)
        cmd-parts (str/split command #" ")
        all-args (concat cmd-parts [template-path])
        result (apply shell {:out :string :in (slurp input-path)} 
                      dictim-cmd all-args)
        output (:out result)]
    ;; Ensure outputs directory exists
    (.mkdirs (io/file "test/cmd/outputs"))
    (spit output-path output)
    (assert (trim= output (slurp expected-path))
            (str "Test failed: " test-name))))

(defn test-validation-error [test-name input-file]
  (let [input-path (str "test/cmd/inputs/" input-file)
        result (shell {:out :string :err :string :continue true
                       :in (slurp input-path)} 
                      dictim-cmd "-val")]
    ;; Should contain "invalid" in the output for validation error
    (assert (re-find #"invalid" (:out result))
            (str "Test failed: " test-name " - expected validation error"))))

(defn cleanup-outputs []
  (doseq [file (file-seq (io/file "test/cmd/outputs"))]
    (when (.isFile file)
      (.delete file))))

;; Test 1: Basic Graphspec Processing
(test-with-files "basic-graphspec" "basic-graphspec.json" "-g" "basic-graphspec.edn")

;; Test 2: Graphspec with Node Template
(test-with-files "graphspec-with-template" "graphspec-with-template.json" "-g" "graphspec-with-template.edn")

;; Test 3: Template Application to D2
(test-with-files-and-template "template-application" "sample.d2" "styling-template.edn" "-a -t" "templated.d2")

;; Test 4: Compilation with Template
(test-with-files-and-template "compilation-with-template" "simple-dictim.edn" "simple-template.edn" "-c -t" "compiled-with-template.d2")

;; Test 5: Validation (Valid Input)
(test-with-files "validation-success" "valid-dictim.edn" "-val" "validation-success.txt")

;; Test 6: Validation (Invalid Input)
(test-validation-error "validation-error" "invalid-dictim.edn")

;; Test 7: Parse with Style Removal
(test-with-files "parse-style-removal" "styled.d2" "-p -r" "clean-structure.edn")

;; Test 8: String Key Conversion
(test-with-files "string-key-conversion" "keyword-dictim.edn" "-st" "string-keys.edn")

;; Clean up output files
(cleanup-outputs)
