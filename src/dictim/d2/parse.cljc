(ns dictim.d2.parse
  {:author "judepayne"
   :doc "Namespace for parsing d2 and returning dictim format."}
  (:require [clojure.string :as str]
            #?(:clj [instaparse.core :as insta :refer [defparser]]
               :cljs [instaparse.core :as insta :refer-macros [defparser]])
            [dictim.d2.attributes :as at]
            [dictim.utils :refer [error try-parse-primitive]]))

;; d2 parser v2

;; This rewrite of the parser focuses on good future extensibility. To support that;
;; - Regexes are kept to a minimum and where used, are formed clearly outside of the grammar.
;; - Different grammar concepts are differentiated explicitly, rather than the v1 approach
;;   that minimised the length of the grmmar but clarity suffered from opaque re-use.
;;   For instance, in v2, vars, classes and connection-references are all explicit
;;   concepts discrete from attrs which they resemble. This approach will allow for
;;   future specific fine tuning of the grammar. The downside to this approach is that
;;   the grammar is longer than before.


;; ------ Literals -------
;; chars & words used as token literals and in regexes in the grammar
;; :reg is the regex value (with proper escaping for instaparse)
;; :lit is the value to be used as a literal (if different to :reg)
;; :hide? indicates whether to token should be hidden e.g. <hidden-token>
;; :not-lit? means not to be used as a literal inserted into the grammar
(def ^:private  char-literals
  {"hash" {:reg "#" :hide? true}
   "period" {:reg "." :hide? true}
   "colon" {:reg ":" :hide? false}
   "semi" {:reg ";" :hide? true}
   "curlyo" {:reg "{" :hide? false}
   "curlyc" {:reg "}" :hide? false}
   "bracketo" {:reg "(" :hide? false}
   "bracketc" {:reg ")" :hide? false}
   "glob" {:reg "*" :hide? true}
   "amp" {:reg "&" :hide? true}
   "single-quote" {:reg "\\'" :not-lit? true}
   "double-quote" {:reg "\\\"" :not-lit? true}
   "hyphen" {:reg "\\-" :hide? true :lit "-"}
   "l-arrow" {:reg "<" :hide? true}
   "r-arrow" {:reg ">" :hide? true}
   "line-return" {:reg "\\n" :not-lit? true}
   "pipe" {:reg "|" :hide? false}
   "dollar" {:reg "$" :not-lit? true}
   "sq-bracketo" {:reg "\\[" :not-lit? true :lit "["}
   "sq-bracketc" {:reg "\\]" :not-lit? true :lit "]"}})


(def ^:private word-literals
  {"dir-left" {:reg "<-" :not-lit? true}
   "dir-right" {:reg "->" :not-lit? true}
   "dir-both" {:reg "<->" :not-lit? true}
   "dir-neither" {:reg "--" :not-lit? true}
   "vars-lit" {:reg "vars" :hide? false}
   "classes-lit" {:reg "classes" :hide? false}
   "ts-open" {:reg ["|||" "|`"] :hide? false}
   "ts-close" {:reg ["|||" "`|"] :hide? false}
   "null" {:reg "null" :hide? false}})


(def ^:private literals (merge char-literals word-literals))


;; the sets of 'banned' chars required in the grammar.
(def ^:private base-bans
  ["period" "semi" "colon" "line-return" "curlyo" "curlyc"])

(def ^:private ctr-key-bans
  (concat base-bans
          ["glob" "amp" "l-arrow" "r-arrow"]))

(def ^:private conn-key-bans
  (concat base-bans
          [ "l-arrow" "r-arrow" "amp" "bracketo" "bracketc"]))

(def ^:private attr-key-bans
  (concat base-bans
          ["bracketo" "bracketc"]))  ;;faciliatate differentiation from conn-refs

(def ^:private attr-val-bans ["glob" "curlyo" "curlyc" "semi" "line-return"])

(def ^:private conn-ref-key-bans
  (concat conn-key-bans ["bracketo" "bracketc"]))

(def ^:private label-bans
  ["semi" "pipe" "curlyo" "curlyc" "line-return"])

(def ^:private inner-list-item-bans ["semi" "sq-bracketo" "sq-bracketc" "line-return"])

(def ^:private dirs ["dir-left" "dir-right" "dir-both" "dir-neither"])


;; Functions that generate output to be inserted into the grammar

(defn- insta-reg
  "Generates a regex that matches any chars apart from the banned;
   a sequence of char names. See map 'insta-quoted-chars' above.
   When a sequence of :negative-lookaheads are also supplied, these
   are added to negative lookahead section of the regex and function
   to ban these substrings also. Unlike the chars specified in the
   banned argument, negative-lookaheads can be (one than one character)
   strings."
  [banned-chars & {:keys [banned-words]
                   :or {banned-words nil}}]
  (let [chars' (map #(-> (get literals %) :reg) banned-chars)
        char-ban-reg (str "[^" (apply str chars') "]")]
    (if banned-words
      (let [neg-literals (map #(-> (get literals %) :reg) banned-words)
            alt-literals (apply str (interpose "|" neg-literals))]
        (str "#'((?!(?:" alt-literals "))" char-ban-reg ")+'"))
      (str "#'" char-ban-reg "+'"))))


(defn- format-literal-val
  [lit]
  (if (vector? lit)
    (apply str (interpose " | "
                          (map #(str "'" % "'") lit)))
    (str "'" lit "'")))


;; The literals chunk of the grammar
(def ^:private literals-insert
  (apply str
         (interpose "\n    "
                    (map (fn [[k m]]
                           (let [v-lit (get m :lit (get m :reg))]
                             (str (if (:hide? m)
                                    (str "<" k ">")
                                    k)
                                  " = "
                                  (format-literal-val v-lit))))
                         (filter (fn [[_ v]] (not (:not-lit? v))) literals)))))


(defn- grammar []
  (str
   "(* high level structure *)
    <D2> = elements
    elements = break* (element break)* element? | break* | element
    <contained> = (element break)* element? | break* | element
    <element> = list | elem

    <elem> = classes | vars | ctr | attr | comment | conn | conn-ref

    (* lists and comments *)
    list = (elem <semi>+)+ elem <semi>*
    comment = <s> <hash> lbl

    (* containers - including shapes *)
    ctr = (<s> ctr-key colon-label? (<curlyo> <break?> contained <s> <curlyc>)?) | composite-ctr
    <composite-ctr> = <s> ctr-key <period> composite-ctr-attr
    composite-ctr-attr = std-attr      
    ctr-key = !classes-lit !hash !vars-lit (ctr-key-part <period>)* ctr-key-part
    ctr-key-part =  !d2-keyword " (insta-reg ctr-key-bans :banned-words dirs) "

    (* vars, classes and attrs *)
    (* vars *)
    vars = <s> vars-lit <s> <colon> <s> vars-content
    <vars-content> = <curlyo> <break?> the-vars <s> <break>? <s> <curlyc>
    the-vars = (var <break>)* var
    var = <s> ctr-key <colon> <s> (var-val | vars-content)
    var-val = "(insta-reg attr-val-bans) " | inner-list

    (* classes *)
    classes = <s> classes-lit <s> <colon> <s> <curlyo> <at-sep*>
              (class <at-sep*>)* <curlyc>
    class = <s> ctr-key <s> <colon> <s> attrs

    (* attrs *)
    <at-sep> = break | semi
    attr = std-attr
    <std-attr> = (<s> attr-key <s> <colon> <s> (attr-val | attr-label? attrs))
    attrs = <curlyo> <at-sep*> (attr <at-sep+>)* attr <at-sep*> <s> <curlyc>
    attr-key = ((attr-key-part <period>)* (attr-key-last <period>)* attr-key-last) | globs
    attr-key-part = &glob " (insta-reg attr-key-bans :banned-words ["vars-lit"]) "
    attr-key-last = d2-keyword | amp d2-keyword
    attr-label = label (* i.e. lbl, block or typescript *)
    <item> = " (insta-reg inner-list-item-bans) "
    inner-list = <'['> (item <semi> <s>)* item <']'>
    <av> = !null " (insta-reg attr-val-bans) "
    attr-val = av | substitution | <s> null <s> | inner-list

    (* labels *)
    <colon-label> = (<colon> <s> | <colon> <s> label)
    label = lbl | block | typescript
    <lbl> = (<s> null <s>) | normal-label | substitution
    <normal-label> = !null " (insta-reg label-bans) "
    <substitution> =  <s> #'^(?!^\\s*$)([^;${\n]*\\$\\{[^}]+\\})+[^{}\\n;|]*'
    block = <s> pipe #'[^|]+' pipe <s>
    typescript = <s> ts-open #'[\\s\\S]+?(?=\\|\\|\\||`\\|)' ts-close <s>

    (* connections *)
    conn = <s> (conn-key dir)+ <s> conn-key colon-label? attrs?
    conn-key = (conn-key-part <period>)* conn-key-part
    conn-key-part = !d2-keyword " (insta-reg conn-key-bans :banned-words dirs) " (* greedy regex *)
    dir = <contd?> <s> direction
    contd = #'--\\\\\n'
    <direction> = '--' | '->' | '<-' | '<->'

    (* conn-refs - a special type of connection *)
    conn-ref = <s> conn-ref-key conn-ref-val
    conn-ref-key = <'('> <s> crk <s> dir <s> crk <s> <')'> <'['> array-val <']'>
    conn-ref-val = (conn-ref-attr-keys <s> <colon> <s> (attr-val | attrs) | <s> <colon> <s> null)
    crk = " (insta-reg conn-ref-key-bans :banned-words dirs) "
    conn-ref-attr-keys = (<period> d2-keyword)+
    array-val = #'\\d' | globs

    (* building blocks *)
    <any> = #'.'
    <d2-keyword> =(" (at/d2-keys) ")
    s = #' *'
    globs = glob+
    break = lr+
    lr = #'[^\\S\\r\\n]*\\r?\\n'

    (* literals *)
    <single-quote> = '\\''   (* can't insert due to clojure/ instaparse escaping diffs *)
    <double-quote> = '\\\"'  (* ditto *)
    " literals-insert "
    "))


(defparser ^{:doc "A parser for d2" :private true} parse-d2 (grammar))
;; for testing local pod-babshka-instaparse development
#_(require '[babashka.pods :as pods])
#_(pods/load-pod "../pod-babashka-instaparse/pod-babashka-instaparse")
#_(require '[pod.babashka.instaparse :as insta])
#_(def parse-d2 (insta/parser (grammar)))


;; Useful functions to debug the output of parsing
#?(:clj
   (defn parses-d2
     "Implementation detail: exposed for testing purposes only"
     [d2 & kvs]    
     (let [parser (insta/parser (grammar))]
       (apply insta/parses parser d2 kvs))))


#?(:clj
   (defn num-parses
     "Implementation detail: exposed for testing purposes only"
     [d2 & kvs]
     (count (apply parses-d2 d2 kvs))))


(defmacro ^:private dbg [body]
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
        process-empty-lines (fn [parts]
                              (filter
                               (fn [elem]
                                 (cond
                                   (not (vector? elem))               true
                                   
                                   (not= :empty-lines (first elem))   true

                                   (= 0 (second elem))                false

                                   retain-empty-lines?                true

                                   :else                              false))
                               parts))]
    (if (insta/failure? p-trees)
      (throw (error (str "Could not parse: " (:text p-trees))))
      (mapcat
       (fn [p-tree]
         (insta/transform

          {:ctr-key-part identity
           :ctr-key (fn [& parts] (key-fn (apply str (interpose "." parts))))
           :composite-ctr-attr (fn [at-k at-v] {at-k at-v})
           :attr-key-part (fn [& chars] (str/trim (str/join chars)))
           :attr-key-last (fn [& parts] (str/join parts))
           :attr-key (fn [& parts] (key-fn (apply str (interpose "." parts))))
           :inner-list (fn [& items]
                         (into [] (cons :list items)))
           :attr-val (fn [v]
                       (if (nil? v)
                         nil
                         (try-parse-primitive v)))
           :conn-ref (fn [rk rv] (conj rk rv))
           :conn-ref-key (fn [crk1 dir crk2 ar-val]
                           [(key-fn crk1) dir (key-fn crk2) [ar-val]])
           :conn-ref-val (fn
                           ([null] null)
                           ([p1 p2] {p1 p2}))
           :conn-ref-attr-keys (fn [& ks] (str/join (interpose "." ks)))
           :null (constantly nil)
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
           :label (fn [& parts] (if (and (= 1 (count parts))
                                         (nil? (first parts)))
                                  nil  ;; the null label
                                  (label-fn (str/join parts))))
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
           :el (constantly :el)
           :break (fn [& els] [:empty-lines (dec (count els))])
           :attr-label identity
           :attr (fn
                   ([k v] (with-tag {k v} :attrs))
                   ([k lbl m] ;; in-attr-label
                    (with-tag {k (assoc m (key-fn "label") (str/trim lbl))} :attrs)))
           :glob identity
           :globs (fn [& gs] (str/join gs))
           :amp identity
           :attrs (fn [& attrs] (with-tag (into {} attrs) :attrs))
           :ctr
           (fn [& parts]
             (let [parts (process-empty-lines parts)
                   tag (cond
                         (str/includes? (first parts) ".")    :ctr
                         (every? (complement vector?) parts)  :shape
                         :else :ctr)]
               (if (= :shape tag)
                 (let [[kl ms] (split-with (complement map?) parts)
                       attrs (apply merge ms)]
                   (with-tag (if attrs (conj (into [] kl) attrs) (into [] kl)) tag))
                 (with-tag (vec parts) tag))))
            :elements (fn [& elems] (process-empty-lines elems))}

          p-tree))
       p-trees))))
