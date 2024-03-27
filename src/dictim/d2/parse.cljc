(ns dictim.d2.parse
  {:author "judepayne"
   :doc "Namespace for parsing d2 and returning dictim format."}
  (:require [clojure.string :as str]
            #?(:clj [instaparse.core :as insta :refer [defparser]]
               :cljs [instaparse.core :as insta :refer-macros [defparser]])
            [dictim.attributes :as at]
            [dictim.utils :refer [error try-parse-primitive]]))

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
   '("--" "->" "<-" "\\*")
   ":;.\n>"
   false))


(def ekey-reg
  (reg-gen
   at/d2-attributes
   nil
   ".\\-<>\n()"
   true))


;; d2's grammar

(defn- grammar-old []
  (str
   "<D2> = elements
    elements = <sep*> (element (empty-lines | <sep>))* element <sep*>
    <contained> = <sep*> (element (empty-lines | <sep>))* element <sep*>
    <element> = list | elem

    <elem> = var-root | ctr | comment | attr | conn

    list = (elem <semi>+)+ elem <semi>*
    ctr = key colon-label? (<curlyo> <sep*> contained <s> <curlyc>)?
    comment = <s> <hash> label

    attrs = <curlyo> <at-sep*> (attr <at-sep+>)* attr <at-sep*> <s> <curlyc>
    attr = <s> (at-key | conn-ref) <s> <colon> <s> (val | attr-label? attrs)
    attr-label = label

    conn-ref = <'('> <s> ref-key <s> dir <s> ref-key <s> <')'>
               <'['> array-val <']'> conn-ref-attr-keys?
    conn-ref-attr-keys = (<period> d2-keyword)+
    ref-key = #'^[^;:.\\n\\<>\\-)]+'
    array-val = #'(\\d|\\*)+'

    vars = <curlyo> <at-sep*> (var <at-sep+>)* var <at-sep*> <s> <curlyc>
    var-root = <s> 'vars' <s> <colon> <s> vars
    var = <s> var-key <s> <colon> <s> (val | vars)
    var-key = !hash vk
    <vk> = #'[^:;\\n{}]+'
    var-reserved = 'vars'
 
    conn = <s> (ekey dir)+ <s> key colon-label? attrs?
    dir = <contd?> <s> direction
    contd = #'--\\\\\n'
    <direction> = '--' | '->' | '<-' | '<->'
 
    (* keys *)
    glob = '*'
    amp = '&'
    key = !hash !var-reserved (key-part period)* key-part-last
    <key-part> = #'^(?!.*(?:-[>-]|<-))[^\\'\\\";:.\\n]+'
    <key-part-last> = <s> #'" key-reg "'
    at-key = (at-part period)* at-part-last
    <at-part> = key-part | d2-keyword | glob
    <at-part-last> = amp? d2-keyword | glob
    ekey = !hash (ekey-part period)* ekey-part-last
    <ekey-part> = #'^[^;:.\\n\\-<\\[(]+'
    <ekey-part-last> = <s> #'" ekey-reg "'

    (* labels *)
    <labels> = label | block | typescript
    label = lbl | subst
    <subst> = <s> #'^(?!^\\s*$)([^${\n]*\\$\\{[^}]+\\})+[^{}\\n;|]*'
    <lbl> = <s> #'^(?!^\\s*$)[^;|{}\\n]+'
    val = v | subst
    <v> = <s> #'^(?!^\\s*$)[^;|{}\\n]+'
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


;; ctr-keys, conn-keys, attr-keys. unclear about single-quote
(def all-key-bans
  ["hash" "period" "semi" "colon" "line-return" "double-quote"])

(def ctr-key-bans
  (concat all-key-bans
          ["glob" "vars-lit" "amp"]))

(def conn-key-bans
  (concat all-key-bans
          ["hyphen" "l-arrow" "r-arrow" "vars-lit" "amp"]))

(def attr-key-bans all-key-bans)


(defn alt [alts]
  (str "("
       (apply str
              (interpose " | " alts))
       ")"))

(defn neg [s] (str "!" s))


(defn- grammar []
  (str
   "<D2> = elements
    elements = <sep*> (element (empty-lines | <sep>))* element <sep*>
    <contained> = <sep*> (element (empty-lines | <sep>))* element <sep*>
    <element> = elem

    <elem> = ctr

 
    ctr = key colon-label? (<curlyo> <sep*> contained <s> <curlyc>)?
 
    vars-lit = 'vars'

    (* keys *)
    glob = '*'
    key = (" (-> ctr-key-bans alt neg) " ctr-key period)* key-part-last
    ctr-key = #'.'* 
    <key-part> = #'^(?!.*(?:-[>-]|<-))[^\\'\\\";:.\\n]+'
    <key-part-last> = <s> #'" key-reg "'

    boo = (!boo-banned boo-allowed)+
    boo-banned = " (alt ctr-key-bans) "
    <boo-allowed> = #'.'
    


    (* labels *)
    <labels> = label | block | typescript
    label = lbl | subst
    <subst> = <s> #'^(?!^\\s*$)([^${\n]*\\$\\{[^}]+\\})+[^{}\\n;|]*'
    <lbl> = <s> #'^(?!^\\s*$)[^;|{}\\n]+'
    val = v | subst
    <v> = <s> #'^(?!^\\s*$)[^;|{}\\n]+'
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
    <single-quote> = '\\''
    <hyphen> = '-'
    <l-arrow> = '<'
    <r-arrow> = '>'
    <double-quote> = '\\\"'
    <line-return> = '\\n'

    s = #' *'
    <d2-keyword> =" (at/d2-keys)))


#?(:bb (defn parse-d2 [d2] (insta/parse (insta/parser (grammar)) d2))
   :clj (defparser ^{:doc "A parser for d2" :private true} parse-d2 (grammar))
   :cljs (defparser ^{:doc "A parser for d2" :private true} parse-d2 (grammar)))

(defmacro dbg [body]
  `(let [x# ~body]
     (println "dbg:" '~body "=" x#)
     x#))

(defn cond-keyword
  "Converts to keyword where possible."
  [item]
  (cond
    (and (string? item) (str/includes? item " "))    item
    (string? item)                                   (keyword item)
    :otherwise item))


;; a function that is only uses by the parse-test namespace
#?(:clj
   (defn num-parses [d2]
     (-> (insta/parser (grammar))
         (insta/parse d2)
         count)))


(defn- conn-ref? [k]
  (and (vector? k)
       (= :conn-ref (first k))))


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
                                                    (seqable? item)
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
           :val (fn [v] (try-parse-primitive v))
           :attr-label identity
           :attr (fn
                   ([k v] (if (not (conn-ref? k))
                            (with-tag {k v} :attrs)
                            (let [[_ k1 dir k2 ar-val at] k]
                              (with-tag
                                {[k1 dir k2 [ar-val]] {at v}}
                                :conn-ref))))
                   ([k lbl m] ;; in-attr-label
                    (with-tag {k (assoc m (key-fn "label") (str/trim lbl))} :attrs)))
           :glob (fn [g] g)
           :amp (fn [a] a)
           :attrs (fn [& attrs] (with-tag (into {} attrs) :attrs))
           :var-key (fn [v-k] v-k)
           :var (fn [k v] (with-tag {k v} :vars))
           :vars (fn [& vars] (with-tag (into {} vars) :vars))
           :var-root (fn [k v] (with-tag {k v} :vars))
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
                                        ;           :conn-ref (fn [& parts] (str/join parts))
           :ref-key (fn [k] (try-parse-primitive (str/trim k)))
           :array-val (fn [ar-val] (try-parse-primitive ar-val))
           :conn-ref-attr-keys (fn [& parts]
                                 (str/join (interpose "." parts)))
           :empty-lines (fn [& seps] (into [:empty-lines (dec (count seps))]))
           :elements (fn [& elems] (vec (handle-empty-lines elems)))
           :contained (fn [& elems] (println elems) (vec (handle-empty-lines elems)))}

          p-tree))
       p-trees))))
