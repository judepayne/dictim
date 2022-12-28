(ns dictim.parser
  (:require [clojure.string :as str]
            [instaparse.core :as insta]))


;; d2 is quite flexible in providing different ways to express the same thing
;; It's also quite tolerant in how expressions are ended - whether newlines
;; need to be used or not.


;; no way to avoid parser knowing set of d2 reserved keywords
;; to differentiate between element (e.g. shape) and attributes inside container
;; {} used as both scope and map

;; what is a sequence diagram? what is the scope - fragile. make container since we can already suppress the label by setting to ''

;; colon and label - don't allow colon if no label
;; should labels just be regularised inside attr maps

;; foo.fill as previously noted
;;

;; are multiple edges in a line that useful since lavels & other attributes cant be set

;; should labels have keys? even generated ones? optional keys


;; attributes need to be clear what they refer to
;; shape: sequence_diagram
;; a -> b
;; b -> a: hello
;; shape: person
;; c

;; is it helpful to have label as a special attribute - especially as creates confusion with attributes

;; style keys are not checked and errors reported



(def clj
  "a parser for d2"
  (insta/parser
   "<D2> = elem+   (* d2 is made of multiple elemnts *)
    (* each element can be one of these four types.. *)
    <elem> = (attr sep) / shape / conn / ctr 

    (* containers *)
    ctr = spc? key (<':'> | <':'> spc? label)? spc? open elem+ close sep

    (* shapes *)
    shape = spc? key (<':'> | <':'> spc? label)? spc? (open attrs close)? sep

    (* connections *)
    <conn> = single-conn / multi-conn
    single-conn = spc? key spc? dir spc? key spc? (<':'> | <':'> spc? label)?
                  (open attrs close)? sep
    multi-conn = spc? edge+ key sep
    <edge> = key spc? dir spc?
    dir = '--' | '->' | '<-' | '<>'

    (* attributes *)
    attrs = (attr sep)* attr
    attr = spc? d2-key <':'> spc? (val | open attrs spc? newline? close)
    d2-style = 'style' 
    d2-key = d2-word | (key dot d2-word) 
    d2-word = 'shape'|'label'|'source-arrowhead'|'target-arrowhead'|'style'|'near'|
             'icon'|'width'|'height'|'constraint'|'direction'|'opacity'|'fill'|
             'stroke'|'stroke-width'|'stroke-dash'|'border-radius'|'font-color'|
             'shadow'|'multiple'|'3d'|'animated'|'link'
   
    (* building blocks *)
    dot = '.'
    <spc> = <#'\\s'*>
    (* sep terminates an element. the lookahead to closing brace option is
       required for the last in a series of nested elements *) 
    <sep> = <';' | newline | &close>
    <newline> = <#'\\n'>
    <open> = <'{'>
    <close> = <'}'>
    key = #'[0-9a-zA-Z_.\\s]+'
    val = #'[0-9a-zA-Z_\"\\'#]+'
    label = #'^[^-\\s:][0-9a-zA-Z_\\s]+[^\\s\\n{};]'"))


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
