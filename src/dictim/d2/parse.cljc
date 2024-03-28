(ns dictim.d2.parse
  {:author "judepayne"
   :doc "Namespace for parsing d2 and returning dictim format."}
  (:require [clojure.string :as str]
            #?(:clj [instaparse.core :as insta :refer [defparser]]
               :cljs [instaparse.core :as insta :refer-macros [defparser]])
            [dictim.attributes :as at]
            [dictim.utils :refer [error try-parse-primitive]]))

;; d2's grammar

;; the sets of banned chars required in the grammar.
(def base-bans
  ["hash" "period" "semi" "colon" "line-return" "double-quote"
   "curlyo" "curlyc"])


(def ctr-key-bans
  (concat base-bans
          ["glob" "amp" "l-arrow" "r-arrow"]))


(def conn-key-bans
  (concat base-bans
          ["hyphen" "l-arrow" "r-arrow" "amp"]))


(def attr-key-bans
  (concat base-bans
          ["bracketo" "bracketc"]))  ;;faciliatate differentiation from conn-refs


(def attr-val-bans ["semi" "line-return"])


(def conn-ref-key-bans conn-key-bans)


(def label-bans base-bans)


(def substitution-bans base-bans)


(defn alt
  "Return an instaparse expression which alts the input."
  [alts]
  (str "(" (apply str (interpose " | " alts)) ")"))


(def bans
  {"hash" "#"
   "period" ""
   })


(defn reg-banned [bans]
  )


(defn- grammar []
  (str
   "(* high level structure *)
    <D2> = elements
    elements = <sep*> (element (empty-lines | <sep>))* element <sep*>
    <contained> = <sep*> (element (empty-lines | <sep>))* element <sep*>
    <element> = list | elem

    <elem> = vars | ctr | attr | comment | conn

    comment = <s> <hash> lbl
    list = (elem <semi>+)+ elem <semi>*

    (* containers - including shapes *)
    ctr = ctr-key colon-label? (<curlyo> <sep*> contained <s> <curlyc>)?
    ctr-key = (ctr-key-part <period>)* ctr-key-part
    ctr-key-part =  !(contains-dir | vars-lit | d2-keyword)
                     (!ctr-key-banned-chars any)+
    ctr-key-banned-chars = " (alt ctr-key-bans)  "
    
    (* vars and attrs *)
    vars = <s> vars-lit <s> <colon> <s> (attr-val | attr-label? attrs)
    attr = <s> attr-key <s> <colon> <s> (attr-val | attr-label? attrs)
    attrs = <curlyo> <at-sep*> (attr <at-sep+>)* attr <at-sep*> <s> <curlyc>
    attr-key = (attr-key-part <period>)* attr-key-last | conn-ref
    attr-key-part = !vars-lit (!attr-key-banned-chars any <s>)+
    attr-key-last = d2-keyword
    attr-key-banned-chars = " (alt attr-key-bans)  "
    attr-label = label (* i.e. lbl, block or typescript *)
    attr-val-banned-chars = " (alt attr-val-bans) "
    attr-val = (!attr-val-banned-chars any)+

    (* conn-refs - a special type of attr *)
    conn-ref = <'('> <s> conn-ref-key <s> dir <s> conn-ref-key <s> <')'>
               <'['> array-val <']'> conn-ref-attr-keys?
    conn-ref-key = (!conn-ref-key-banned-chars any <s>)+
    conn-ref-attr-keys = (<period> d2-keyword)+
    conn-ref-key-banned-chars = " (alt conn-ref-key-bans)  "
    array-val = #'\\d'

    (* labels *)
    <colon-label> = (<colon> <s> | <colon> <s> label)
    label = lbl | block | typescript
    lbl = lbl-part | substitution | (lbl-part? substitution lbl-part?)+
    lbl-part = #'[^;|{}\\n]+' (* greedy regex to avoid parser ambiguity *)
    lbl-banned-chars = " (alt label-bans) "                
    substitution = '${' (!substitution-banned-chars any)+ '}'
    substitution-banned-chars = " (alt substitution-bans) "
    block = <s> pipe (!pipe any)+ pipe <s>
    typescript = <s> ts-open (!pipe any)+ ts-close <s>

    (* connections *)
    conn = <s> (conn-key dir)+ <s> conn-key colon-label? attrs?
    conn-key = (conn-key-part <period>)* conn-key-part
    conn-key-part = !d2-keyword (!conn-key-banned-chars any <s>)+
    conn-key-banned-chars = " (alt conn-key-bans) "
    dir = <contd?> <s> direction
    contd = #'--\\\\\n'
    <direction> = '--' | '->' | '<-' | '<->'

    (* regex bans: for when single char bans are insufficient  *)
    contains-dir = #'^.*(--|->|<-|<->).*$'

    (* building blocks *)
    <any> = #'.'
    empty-lines = sep sep+
    sep = <#'[^\\S\\r\\n]*\\r?\\n'>
    <at-sep> = sep | semi
    vars-lit = 'vars'
    colon = ':'
    <semi> = ';'
    <hash> = '#'
    curlyo = '{'
    curlyc = '}'
    bracketo = '('
    bracketc = ')'
    <period> = '.'
    <glob> = '*'
    <amp> = '&'
    <single-quote> = '\\''
    <hyphen> = '-'
    <l-arrow> = '<'
    <r-arrow> = '>'
    <double-quote> = '\\\"'
    <line-return> = '\\n'
    pipe = '|'
    ts-open = '|||' | '|`'
    ts-close = '|||' | '`|'
    s = #' *'
    <d2-keyword> =(" (at/d2-keys) ")"))


"olly: |jude ${cat} me ${dog}| {style: 1}\n
#I'm a comment\n jude: simple man {Bridget: lovely daughter\nfill: pink}\n\n"

#?(:bb (defn parse-d2 [d2] (insta/parse (insta/parser (grammar)) d2))
   :clj (defparser ^{:doc "A parser for d2" :private true} parse-d2 (grammar))
   :cljs (defparser ^{:doc "A parser for d2" :private true} parse-d2 (grammar)))

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
         (insta/parses d2)
         count)))


(defn- conn-ref? [k]
  (and (vector? k)
       (= :conn-ref (first k))))


(defmacro dbg [body]
  `(let [x# ~body]
     (println "dbg:" '~body "=" x#)
     x#))


(defn dbg-identity [arg]
  (println arg)
  (identity arg))

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

          {:ctr-key-part (fn [& chars] (str/trim (str/join chars)))
           :ctr-key (fn [& parts] (key-fn (apply str (interpose "." parts))))
           :attr-key-part (fn [& chars] (str/trim (str/join chars)))
           :attr-key-last identity
           :attr-key (fn [& parts] (key-fn (apply str (interpose "." parts))))
           :attr-val (fn [& chars]
                       (-> chars
                           str/join
                           try-parse-primitive))
           :conn-ref-key identity
           :vars-lit (constantly "vars")
           :var-key (fn [v-k] v-k)
           :vars (fn [k v] (with-tag {(key-fn k) v} :vars))
           :substitution (fn [& chars] (str/join chars))
           :lbl-part identity
           :lbl (fn [& parts] (label-fn (str/join parts)))
           :label identity
           :ts-open identity
           :ts-close identity
           :typescript (fn [& parts] (str/join parts))
           :pipe identity
           :block (fn [& parts] (str/join parts))
           :comment (fn [c] (with-tag [:comment (str/triml c)] :comment))
           :list (fn [& elems]
                   (let [elems'
                         (if (and flatten-lists?
                                  (every? (fn [item] (= 1 (count item))) elems))
                           (map first elems)
                           elems)]
                     (with-tag (into [:list] elems') :list)))
           :dir identity
           :conn-key (fn [& parts] (key-fn (str/join (interpose "." parts))))
           :conn-key-part (fn [& parts] (str/join parts))
           :conn (fn [& parts] (with-tag (vec parts) :conn))
           :array-val (fn [ar-val] (try-parse-primitive ar-val))
           :conn-ref-attr-keys (fn [& parts]
                                 (str/join (interpose "." parts)))           


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


           :empty-lines (fn [& seps] (into [:empty-lines (dec (count seps))]))
           :elements (fn [& elems] (vec (handle-empty-lines elems)))
           :contained (fn [& elems] (println elems) (vec (handle-empty-lines elems)))}

          p-tree))
       p-trees))))
