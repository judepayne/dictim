#!/usr/bin/env bb

;; test script to be run in .circleci environment
;; assume: bb, bbin & dictim installed

(require '[babashka.process :refer [shell]])
(require '[clojure.edn :refer [read-string]])

;; test that dictim is installed
(assert (:out (shell {:out :string} "dictim" "-v")))


(def dict [:plan "build the resource"])
(def d2 "plan: build the resource")

;; test compilation
(assert (= (:out (shell {:out :string :in (pr-str dict)} "dictim" "-c"))
           (str d2 "\n")))


;; test parsing
(assert (= (read-string (:out (shell {:out :string :in d2} "dictim" "-k" "-p")))
             dict))
