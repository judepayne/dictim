(ns dictim.d2.parse
  {:author "judepayne"
   :doc "Namespace for parsing d2 and returning dictim format."}
  (:require [clojure.string :as str]
            #?(:clj [instaparse.core :as insta :refer [defparser]]
               :cljs [instaparse.core :as insta :refer-macros [defparser]])
            #?(:clj [lambdaisland.deep-diff2 :as ddiff])
            [dictim.attributes :as at]
            [dictim.utils :refer [error try-parse-primitive line-by-line]]))

;; d2's grammar

;; d2 has a fairly free-form grammar. i.e. lots of options for expressing something.
;; In this grammar, we've tried to keep the use of regexes down to a minimum to
;; keep it more comprehensible/ extensible in the future.
;; d2's grammar is texty and can have a variable amount of whitespace between
;; the different tokens. Whitespace can cause parser ambiguity since instaparse can
;; choose to put intermediate whitespace onto the end of the first token or at
;; the start of the succeeding. In order to keep ambiguity are control,
;; it is occasionally necessary to resort to the use of greedy regex expressions
;; which suck up the target token and following whitespace up until the start
;; of the following token.
;; In order to keep the clarity of the overall grammar, most of the token matching
;; expressions have been broken out of the main grammar itself below...


;; the sets of banned chars required in the grammar.
(def base-bans
  ["period" "semi" "colon" "line-return" "curlyo" "curlyc"])

(def ctr-key-bans
  (concat base-bans
          ["glob" "amp" "l-arrow" "r-arrow"]))

(def conn-key-bans
  (concat base-bans
          ["hyphen" "l-arrow" "r-arrow" "amp"]))

(def attr-key-bans
  (concat base-bans
          ["bracketo" "bracketc"]))  ;;faciliatate differentiation from conn-refs

(def attr-val-bans ["curlyo" "curlyc" "semi" "line-return"])

(def conn-ref-key-bans
  (concat conn-key-bans ["bracketo" "bracketc"]))

(def label-bans base-bans)

(def substitution-bans base-bans)


(defn alt
  "Return an instaparse expression which alts the input."
  [alts]
  (str "(" (apply str (interpose " | " alts)) ")"))

;; The value of each key is escaped as required to be inserted into
;; an instaparse grammar.
(def insta-literals
  {"hash" "#"
   "period" "."
   "colon" ":"
   "semi" ";"
   "curlyo" "{"
   "curlyc" "}"
   "bracketo" "("
   "bracketc" ")"
   "glob" "*"
   "amp" "&"
   "single-quote" "\\'"
   "double-quote" "\\\""
   "hyphen" "\\-"
   "l-arrow" "<"
   "r-arrow" ">"
   "line-return" "\\n"
   "pipe" "|"
   "dollar" "$"
   "dir-left" "<-"
   "dir-right" "->"
   "dir-both" "<->"
   "dir-neither" "--"
   "vars-lit" "vars"})


(defn insta-reg
  "Generates a regex that matches any chars apart from the banned;
   a sequence of char names. See map 'insta-quoted-chars' above."
  [banned]
  (let [chars' (map #(get insta-literals %) banned)]
    (str "#'[^" (apply str chars') "]*'")))


(def dirs ["dir-left" "dir-right" "dir-both" "dir-neither"])


(defn insta-reg
  "Generates a regex that matches any chars apart from the banned;
   a sequence of char names. See map 'insta-quoted-chars' above.
   When a sequence of :negative-lookaheads are also supplied, these
   are added to negative lookahead section of the regex and function
   to ban these substrings also. Unlike the chars specified in the
   banned argument, negative-lookaheads can be (one than one character)
   strings."
  [banned & {:keys [negative-lookaheads]
             :or {negative-lookaheads nil}}]
  (let [chars' (map #(get insta-literals %) banned)
        char-ban-reg (str "[^" (apply str chars') "]")]
    (if negative-lookaheads
      (let [neg-literals (map #(get insta-literals %) negative-lookaheads)
            alt-literals (apply str (interpose "|" neg-literals))]
        (str "#'((?!(?:" alt-literals "))" char-ban-reg ")+'"))
      (str "#'" char-ban-reg "+'"))))


;; attr = normal-attr | conn-ref
;; must include the attr-val in the structure to migrate
;; the conn-ref-attr-keys (d2 keywords) to the val in dictim
;; time processing
;; attr val itself can be a val or attrs, i.e. nested.


(defn- grammar []
  (str
   "(* high level structure *)
    <D2> = elements
    elements = break* (element break)* element? | break* | element
    <contained> = (element break)* element? | break* | element
    <element> = list | elem

    <elem> = classes | vars | ctr | attr | comment | conn

    break = empty-line+ line-return? | line-return
    empty-line = line-return line-return
    line-return = <#'[^\\S\\r\\n]*\\r?\\n'>

    list = (elem <semi>+)+ elem <semi>*
    comment = <s> <hash> lbl

    (* containers - including shapes *)
    ctr = <s> ctr-key colon-label? (<curlyo> <break?> contained <s> <curlyc>)?
    ctr-key = !classes-lit !hash !vars-lit (ctr-key-part <period>)* ctr-key-part
    ctr-key-part =  !d2-keyword " (insta-reg ctr-key-bans :negative-lookaheads dirs) "

    (* vars, classes and attrs *)
    (* vars *)
    vars = <s> vars-lit <s> <colon> <s> vars-content
    <vars-content> = <curlyo> <break?> the-vars <s> <break>? <s> <curlyc>
    the-vars = (var break)* var
    var = <s> ctr-key <colon> <s> (var-val | vars-content)
    var-val = "(insta-reg attr-val-bans) "

    (* classes *)
    classes = <s> classes-lit <s> <colon> <s> <curlyo> <at-sep*>
              (class <at-sep*>)* <curlyc>
    class = <s> ctr-key <s> <colon> <s> attrs

    (* attrs *)
    <at-sep> = break | semi
    attr = std-attr | conn-ref
    <std-attr> = (<s> attr-key <s> <colon> <s> (attr-val | attr-label? attrs))
    attrs = <curlyo> <at-sep*> (attr <at-sep+>)* attr <at-sep*> <s> <curlyc>
    attr-key = (attr-key-part <period>)* attr-key-last
    attr-key-part = " (insta-reg attr-key-bans :negative-lookaheads ["vars-lit"]) "
    attr-key-last = d2-keyword | glob | amp d2-keyword
    attr-label = label (* i.e. lbl, block or typescript *)
    attr-val = "(insta-reg attr-val-bans) " | substitution

      (* conn-refs - a special type of attr *)
    <conn-ref> = <s> conn-ref-key conn-ref-val
    conn-ref-key = <'('> <s> crk <s> dir <s> crk <s> <')'> <'['> array-val <']'>
    conn-ref-val = conn-ref-attr-keys <s> <colon> <s> (attr-val | attrs) 
    crk = " (insta-reg conn-ref-key-bans) "
    conn-ref-attr-keys = (<period> d2-keyword)+
    array-val = #'\\d' | glob

    (* labels *)
    <colon-label> = (<colon> <s> | <colon> <s> label)
    label = lbl | block | typescript
    <lbl> = normal-label | substitution
    <normal-label> = #'[^;|{}\\n]+' (* greedy regex to avoid parser ambiguity *)
    <substitution> =  <s> #'^(?!^\\s*$)([^${\n]*\\$\\{[^}]+\\})+[^{}\\n;|]*'
    block = <s> pipe #'[^|]+' pipe <s>
    typescript = <s> ts-open #'[\\s\\S]+?(?=\\|\\|\\||`\\|)' ts-close <s>

    (* connections *)
    conn = <s> (conn-key dir)+ <s> conn-key colon-label? attrs?
    conn-key = (conn-key-part <period>)* conn-key-part
    conn-key-part = !d2-keyword " (insta-reg conn-key-bans) " (* greedy regex *)
    dir = <contd?> <s> direction
    contd = #'--\\\\\n'
    <direction> = '--' | '->' | '<-' | '<->'

    (* building blocks *)
    <any> = #'.'
    empty-lines = sep sep+
    sep = <#'[^\\S\\r\\n]*\\r?\\n'>
    vars-lit = 'vars'
    classes-lit = 'classes'
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
    pipe = '|'
    ts-open = '|||' | '|`'
    ts-close = '|||' | '`|'
    s = #' *'
    <d2-keyword> =(" (at/d2-keys) ")"))


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


;; Useful functions to debug the output of parsing
#?(:clj
   (defn parses-d2 [d2 & kvs]    
     (let [parser (insta/parser (grammar))]
       (apply insta/parses parser d2 kvs))))


#?(:clj
   (defn num-parses [d2 & kvs]
     (count (apply parses-d2 d2 kvs))))

;; a function used to identify the cause of parser ambiguities
#?(:clj
   (defn ambig [d2]
     (let [parses (-> (insta/parser (grammar))
                      (insta/parses d2))]
       (if (= 1 (count parses))
         nil
         (ddiff/pretty-print (apply ddiff/diff (map first (take 2 parses))))))))


(defn- conn-ref? [k]
  (and (vector? k)
       (= :conn-ref (first k))))


(defmacro dbg [body]
  `(let [x# ~body]
     (println "dbg:" '~body "=" x#)
     x#))


(defn dictim
  "Converts a d2 string to its dictim representation.
   Each dictim element returned's type is captured in the :tag key
   of the element's metadata.
   Three optional functions may be supplied:
     :key-fn             a modifier applied to each key.
     :label-fn           a modifier applied to each label.
     :flatten-lists?     if true, flattens lists where every element is a shape
                         with just a key, & no label or attrs.
     :retain-empty-lines?  If true, empty lines are retained in the dictim output."
  
  [d2 & {:keys [key-fn label-fn flatten-lists? retain-empty-lines?]
         :or {key-fn identity
              label-fn str/trim
              flatten-lists? false
              retain-empty-lines? false}}]
  (let [p-trees (parse-d2 d2)
        key-fn (comp key-fn str/trim)
        with-tag (fn [obj tag] (with-meta obj {:tag tag}))
        process-breaks (fn [parts]
                         (filter
                          (fn [elem]
                            (if (= :empty-line elem)
                              (if retain-empty-lines? true false)
                              true))
                          (filter (complement nil?) parts)))]
    (if (insta/failure? p-trees)
      (throw (error (str "Could not parse: " (-> p-trees last second))))
      (mapcat
       (fn [p-tree]
         (insta/transform

          {:ctr-key-part (fn [& chars] (str/trim (str/join chars)))
           :ctr-key (fn [& parts] (key-fn (apply str (interpose "." parts))))
           :attr-key-part (fn [& chars] (str/trim (str/join chars)))
           :attr-key-last (fn [& parts] (str/join parts))
           :attr-key (fn [& parts] (key-fn (apply str (interpose "." parts))))
           :attr-val (fn [& chars]
                       (-> chars
                           str/join
                           ;;str/trim
                           try-parse-primitive))
           :conn-ref-key (fn [crk1 dir crk2 ar-val]
                           [crk1 dir crk2 [ar-val]])
           :conn-ref-val (fn [p1 p2] {p1 p2})
           :conn-ref-attr-keys (fn [& ks] (str/join (interpose "." ks)))
           :crk (fn [k] (key-fn (str/trim k)))
           :array-val (fn [ar-val] (try-parse-primitive ar-val))
           :vars-lit (constantly "vars")
           :the-vars (fn [& vars] (into {} (concat vars)))
           :var (fn [k v] [k v])
           ;; & _ below to catch :breaks
           :vars (fn [k v & _] (with-tag {(key-fn k) v} :vars))
           :var-val (fn [v] (try-parse-primitive v))
           :class (fn [k v] (with-tag {k v} :class))
           :classes (fn [k & kvs]
                      (with-tag {k (apply merge kvs)} :classes))
           :classes-lit identity
           :label (fn [& parts] (label-fn (str/join parts)))
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
           :line-return (constantly :line-return)
           :empty-line (constantly :empty-line)
           :break (fn [& brks] (first (filter #(= :empty-line %) brks)))
           :elements (fn [& elems] (process-breaks elems))
           :attr-label identity
           :attr (fn
                   ([k v] (with-tag {k v} :attrs))
                   ([k lbl m] ;; in-attr-label
                    (with-tag {k (assoc m (key-fn "label") (str/trim lbl))} :attrs)))
           :glob identity
           :amp identity
           :attrs (fn [& attrs] (with-tag (into {} attrs) :attrs))
           :ctr
           (fn [& parts]
             (let [parts (process-breaks parts)
                   tag (cond
                         (str/includes? (first parts) ".")    :ctr
                         (every? (complement vector?) parts)  :shape
                         :else :ctr)]
               (if (= :shape tag)
                 (let [[kl ms] (split-with (complement map?) parts)
                       attrs (apply merge ms)]
                   (with-tag (if attrs (conj (into [] kl) attrs) (into [] kl)) tag))
                 (with-tag (vec parts) tag))))}

          p-tree))
       p-trees))))
