(ns
    ^{:author "judepayne"
      :doc "Namespace for parsing d2 and returning dictim format."}
    dictim.parse
  (:require [clojure.string :as str]
            [instaparse.core :as insta]
            [dictim.attributes :as at]))


(def ^:private parse-d2
  "a parser for d2"
  (insta/parser
   (str
    "<D2> = elem+   (* d2 is made of multiple elemnts *)
    (* each element can be one of these four types.. *)
    <elem> = (attr sep) / shape / conn / ctr / comment

    (* containers *)
    ctr = key (<':'> | <':'> label)? open elem+ close sep

    (* shapes *)
    shape = key (<':'> | <':'> label)? attr-map? sep

    (* connections *)
    conn = single-conn | multi-conn
    <single-conn> = key dir key (<':'> | <':'> label)? attr-map? sep
    <multi-conn> = edge+ key (<':'> | <':'> label)? attr-map? sep
    <edge> = key dir
    dir = '--' | '->' | '<-' | '<->'

    (* attributes *)
    attr-map = open (attr sep)* attr sep? close
    attr = d2-key <':'> (val | attr-map)
    d2-style = 'style' 
    d2-key = d2-word | (d2-word dot d2-word) 
    <d2-word> = "
    
    (at/d2-keys)       ;; d2 keys separated to make easier to update

    "\n\n"
    "    (* comments *)
    comment = <'#'> cmt sep
   
    (* building blocks *)
    <dot> = '.'
    <spc> = <#'\\s'*>
    (* sep terminates an element. the lookahead to closing brace option is
       required for the last in a series of nested elements *) 
    <sep> = <newline | ';'+ | &close>
    <newline> = <#'\\n'>
    <open> = <'{'>
    <close> = <'}'>
    key = #'[0-9a-zA-Z_. ]+'
    <label> = lbl | empty
    <empty> = <#'[ ]'>
    lbl = #'[0-9a-zA-Z \\'._\\$\\£\\@-]+'
    <cmt> = #'[0-9a-zA-Z \\'._\\?\\!-]+'
    val = #'[0-9a-zA-Z_.\"\\'#]+'")
   :auto-whitespace :standard))


(defn- d2-terminated?
  "Is the string terminated appropriately for d2?"
  [s]
  (case (last s)
    \newline   true
    \;         true
    false))


(defn- terminate
  "Terminate the string if necessary."
  [s]
  (if (d2-terminated? s)
    s
    (str s \newline)))


(defn dictim
  "Converts a d2 string to its dictim representation.
   Two optional functions may be supplied:
     :key-fn     a modifier applied to each key.
     :label-fn   a modifier applied to each label."
  [s & {:keys [key-fn label-fn]
        :or   {key-fn identity
               label-fn str/trim}}]
  (let [p-trees (parse-d2 (terminate s))
        key-fn (comp key-fn str/trim)]
    (map
     (fn [p-tree]
       (insta/transform
        {:comment (fn [c][:comment (str/triml c)])
         :key key-fn
         :lbl label-fn
         :dir identity
         :d2-key (fn [& parts] (key-fn (str/join parts)))
         :val identity
         :attr (fn [k v] {k v})
         :attr-map (fn [& attrs] (into {} attrs))
         :ctr (fn [& parts] (vec parts))
         :conn (fn [& parts] (vec parts))
         :shape (fn [& parts] (vec parts))}
        p-tree))
     p-trees)))
