#!/usr/bin/env bb

;; test script to be run in .circleci environment
;; assume: bb, bbin & dictim installed

(require '[babashka.process :refer [shell]])


;; test that dictim is installed
(assert (:out (shell {:out :string} "dictim -v")))


(def dict [:plan "build the resource"])
(def d2 "plan: build the resource")

;; test compilation

(println (:out (shell {:out :string :in dict} "dictim" "-c")))


#_(assert (= (:out (shell {:out :string :in dict} "dictim" "-c"))
           d2))


;; test parsing
#_(assert (= (:out (shell {:out :string :in d2} "dictim" "-p"))
           dict))
