#!/usr/bin/env bb

;; wrapper script to get rlwrap

(require '[babashka.process :refer [shell]]
         '[babashka.fs :refer [cwd]])


(def cur-dir (str (cwd)))


(apply shell
       "rlwrap"
       "-pBlue"
       "-b" "'()=<>&+*|:;,\\'"
       "bb"
       (str cur-dir "/bin/" "dictim-impl.bb")
       *command-line-args*)
