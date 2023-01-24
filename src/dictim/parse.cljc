(ns dictim.parse
  {:author "judepayne"
   :doc "Namespace for parsing d2 and returning dictim format."}
  (:require [clojure.string :as str]
            #?(:clj [instaparse.core :as insta :refer [defparser]]
               :cljs [instaparse.core :as insta :refer-macros [defparser]])
            [dictim.attributes :as at]
            [dictim.utils :refer [error]]))

;; regexes required to parse d2

(defn- reg-gen
  "Generates a regex (for matching keys) where the string:
   - can't be one of not-in - a sequence of strings
   - can't contain any of the substrings cant-contain
   - matches up until any of the delim-chars"
  [not-in cant-contain delim-chars edge?]
  (let [not-fn
        (fn [ns]
          (apply
           str
           (interpose
            "|"(map
                #(if edge?
                   (str % "|" % "[ ].*")
                   (str % "|" % "[" (str delim-chars) "][\\s\\S]*"))
                ns))))
        cant-fn
        (fn [ns]
          (apply str (interpose "|" (map #(str %) ns))))]
    (str
     "^(?!(?:"
     (not-fn not-in)
     ")$"
     (when cant-contain
       (str "|.*(?:" (cant-fn cant-contain) ")"))
     ")[^"
     delim-chars
     "]+")))


(def key-reg
  (reg-gen
   at/d2-attributes
   '("--" "->" "<-")
   ":;.\n>"
   false))


(def ekey-reg
  (reg-gen
   at/d2-attributes
   nil
   ".\\-<>\n"
   true))

;; Notes on parsing d2
;; d2 is a pretty free in its syntax, has some ambiguity in the
;; grammar and is quite 'texty' so not all that easy to parse.

;; Tokens are not particularly easy to recognize with regexes.
;;
;; A particular source of ambiguity is the similarity between
;; shapes and containers. containers being shapes that happen to
;; have other elements than attr's in them. I've elected to parse
;; shapes and containers in the grammar below as one thing, 'ctr'
;; and them in the dictim function detect whether a shape or
;; container and set the :tag in metadata accordingly. This helped
;; to produce a simpler grmmar.
;; I've tried rewriting the parse a number of times trying to get as
;; much logic out of regexes and into instaparse, but since d2 is so
;; free and essentially terminates tokens (which can be anything) by
;; a few delimiter chars/ strings, unfortunately quite a bit of logic
;; has had to end up in negative lookahead regexes.
;; The only way to tell the difference between a shape e.g. a: A and
;; an attribute e.g. link: A is to know the set of d2 (attribute)
;; keywords rather than by the structure of the language, then to parse
;; the language requires to know the set of (attribute) keywords.
;; This coupling is perhaps not ideal and could be eliminated if
;; d2 were to change its language spec to always have attributes inside
;; their own braces e.g. <link: 42>. 

(defn- grammar []
  (str
   "<D2> = elements
    elements = <sep*> (element (empty-lines | <sep>))* element <sep*>
    <contained> = <sep*> (element (empty-lines | <sep>))* element <sep*>
    <element> = list | elem

    <elem> = ctr |comment | attr | conn

    list = (elem <semi>+)+ elem <semi>*
    ctr = key colon-label? (<curlyo> <sep*> contained <s> <curlyc>)?
    comment = <s> <hash> label

    attrs = <curlyo> <at-sep*> (attr <at-sep+>)* attr <at-sep*> <s> <curlyc>
    attr = <s> at-key <s> <colon> <s> (val | attr-label? attrs)
    attr-label = label
    <val> = label

    conn = <s> (ekey dir)+ <s> key colon-label? attrs?
    dir = <contd?> <s> direction
    contd = #'--\\\\\n'
    <direction> = '--' | '->' | '<-' | '<->'
 
    (* keys *)
    K = key | at-key
    key = !hash (key-part period)* key-part-last
    <key-part> = #'^(?!.*(?:-[>-]|<-))[^\\'\\\";:.\\n]+'
    <key-part-last> = <s> #'" key-reg "'
    at-key = (at-part period)* at-part-last
    <at-part> = key-part | d2-keyword
    <at-part-last> = d2-keyword
    ekey = !hash (ekey-part period)* ekey-part-last
    <ekey-part> = #'^[^;:.\\n\\-<]+'
    <ekey-part-last> = <s> #'" ekey-reg "'

    (* labels *)
    <labels> = label | block | typescript
    label = <s> #'^(?!^\\s*$)[^;|{}\\n]+'
    <colon-label> = (<colon> <s> | <colon> labels)
    block = <s> '|' #'[^|]+' '|'
    typescript = <s> ts-open ts ts-close <s>
    <ts> = #'[\\s\\S]+?(?=\\|\\|\\||`\\|)'
    ts-open = '|||' | '|`'
    ts-close = '|||' | '`|'
 
    (* building blocks *)
    <any> = #'.'
    <any-key> = #'[^.:;{\\n]'
    empty-lines = sep sep+
    sep = <#'[^\\S\\r\\n]*\\r?\\n'>
    <at-sep> = sep | semi
    colon = ':'
    <semi> = ';'
    <hash> = '#'
    curlyo = '{'
    curlyc = '}'
    <period> = '.'

    s = #' *'
    <d2-keyword> =" (at/d2-keys)))


(insta/defparser
  ^{:doc "A parser for d2"
    :private true}
  parse-d2
  (grammar))


(defn- num-parses [d2]
  (count (insta/parses parse-d2 d2)))


;; think about always capturing in :attrs in the parse phase. eliminate :attr
;; so attrs would be ::= attr| current multi-attr definition inside {}
;; they would mean in transform phase that attr always came through attrs


(defn dictim
  "Converts a d2 string to its dictim representation.
   Each dictim element returned's type is captured in the :tag key
   of the element's metadata.
   Three optional functions may be supplied:
     :key-fn             a modifier applied to each key.
     :label-fn           a modifier applied to each label.
     :flatten-lists?     if true, flattens lists where every element is a shape
                         with just a key, & no label or attrs.
     :show-empty-lines?  If true, empty lines are hidden into the dictim output."
  
  [d2 & {:keys [key-fn label-fn flatten-lists? show-empty-lines?]
         :or {key-fn identity
              label-fn str/trim
              flatten-lists? false
              show-empty-lines? false}}]
  (let [p-trees (parse-d2 d2)
        key-fn (comp key-fn str/trim)
        with-tag (fn [obj tag] (with-meta obj {:tag tag}))
        handle-empty-lines (fn [elems] (filter
                                        (fn [item]
                                          (not (and (not show-empty-lines?)
                                                        (seq item)
                                                        (= :empty-lines (first item)))))
                                        elems))]

    (if (insta/failure? p-trees)
      (throw (error (str "Could not parse: " (-> p-trees last second))))
      (mapcat
       (fn [p-tree]
         (insta/transform

          {:label (fn [& parts] (label-fn (str/join parts)))
           :block (fn [& parts] (str/join parts))
           :key (fn [& parts] (key-fn (str/join parts)))
           :at-key (fn [& parts] (key-fn (str/join parts)))
           :ekey (fn [& parts] (key-fn (str/join parts)))
           :dir (fn [dir] dir)
           :ts-open (fn [o] o)
           :ts-close (fn [c] c)
           :typescript (fn [& parts] (str/join parts))
           :attr-label identity
           :attr (fn
                   ([k v] (with-tag {k v} :attrs))
                   ([k lbl m] ;; in-attr-label
                    (with-tag {k (assoc m (key-fn "label") (str/trim lbl))} :attrs)))
           :attrs (fn [& attrs] (with-tag (into {} attrs) :attrs))
           :comment (fn [c] (with-tag [:comment (str/triml c)] :comment))
           :ctr (fn [& parts]
                  (let [tag (cond
                              (str/includes? (first parts) ".")    :ctr
                              (every? (complement vector?) parts)  :shape
                              :else :ctr)]
                    (if (= :shape tag)
                      (let [[kl ms] (split-with (complement map?) parts)
                            attrs (apply merge ms)]
                        (with-tag (if attrs (conj (into [] kl) attrs) (into [] kl)) tag))
                      (with-tag (vec (handle-empty-lines parts)) tag))))
           :conn (fn [& parts] (with-tag (vec parts) :conn))
           :list (fn [& elems] (let [elems' (if (and flatten-lists?
                                                     (every? (fn [item] (= 1 (count item))) elems))
                                              (map first elems)
                                              elems)]
                                 (with-tag (into [:list] elems') :list)))
           :empty-lines (fn [& seps] (into [:empty-lines (dec (count seps))]))
           :elements (fn [& elems] (vec (handle-empty-lines elems)))
           :contained (fn [& elems] (println elems) (vec (handle-empty-lines elems)))}

          p-tree))
       p-trees))))
