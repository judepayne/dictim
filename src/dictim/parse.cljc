(ns dictim.parse
  {:author "judepayne"
   :doc "Namespace for parsing d2 and returning dictim format."}
  (:require [clojure.string :as str]
            #?(:clj [instaparse.core :as insta :refer [defparser]]
               :cljs [instaparse.core :as insta :refer-macros [defparser]])
            [dictim.attributes :as at]
            [dictim.utils :refer [error]]))


(defn- reg-gen
  "Generates a regex (for matching keys) where the string:
   - can't be one of not-in - a sequence of strings
   - can't contain any of the substrings cant-contain
   - matches up until any of the delim-chars"
  [not-in cant-contain delim-chars edge?]
  (let [not-fn (fn [ns]
                 (apply str
                        (interpose
                         "|"(map
                             #(if edge?
                                (str % "|" % "[ ].*")
                                (str % "|" % "[" (str delim-chars) "][\\s\\S]*"))
                             ns))))
        cant-fn (fn [ns] (apply str (interpose "|" (map #(str %) ns))))]
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
;; d2 is a pretty free in ita syntax and quite 'texty' so
;; not all that easy to parse.
;; e.g. containers have the same syntax as shapes, but happen
;; to have other elements inside.
;; e.g. many parts of the structure are entirely optional.
;; Tokens are not particularly easy to recognize with regexes.
;;
;; The approach I've taken below to focus on getting keys to be
;; perfectly parsed each time.. since getting keys right and being
;; able to differentiate between a key that belongs to a shape
;; and a key of an attribute ultimately leads to being able to
;; differentiate between shapes and containers (with their similar
;; structure). Since d2 permits almost anything in a key, the
;; answer is to focus on the few delimiter/ 'key-stop' characters
;; that cannot be part of a key and use those to recognize when
;; a key has ended.
;; In addition, d2 cannot be parsed without knowledge of the set
;; of possible attribute keys. This is required to differentiate
;; betwen an attribute and a shape nested inside a container -
;; the structure of the language itself is not sufficient to
;; parse it. Since d2-keys will no doubt be extended in future,
;; I've elected to keep them in just one one place (attributes ns).
;; 
;; I've tried as much as possible to avoid the use of complex
;; regexes to parse d2, keep as much of the description of the
;; language in the EBNF(+) notation of instaparse below.
;; Unfortunately for such a flexible syntax language, this was
;; an intention rather than the outcome!
;; Finally, rather than use the :auto-whitespace feature of d2,
;; I've elected to explicitly mark where white space can be
;; eliminated (the 's' token) as this resulted in (much) less
;; ambiguity in the parse tree, and a faster parse.

(insta/defparser
  ^:private
  p-d2
  (str
   "<D2> = elements
    <elements> = <sep*> (element <sep+>)* element <sep*>
    <element> = list | elem

    <elem> = shape | comment | ctr | attr | conn

    list = (elem <semi>+)+ elem <semi>*

    ctr = key colon-label? <curlyo> elements <s> <curlyc>
    shape = key colon-label-plus? attrs?
    comment = <hash> label

    attrs = <curlyo> <at-sep*> (attr <at-sep+>)* attr <at-sep*> <s> <curlyc>
    attr = <s> at-key <s> <colon> <s> (val | attr-label? attrs)
    attr-label = label
    <val> = label

    conn = <s> (ekey dir)+ <s> key colon-label? attrs?
    dir = <contd?> direction
    contd = #'--\\\\\n'
    <direction> = '--' | '->' | '<-' | '<->'
 
    (* keys *)
    K = key | at-key
    key = !hash (key-part period)* key-part-last
    <key-part> = #'^(?!.*(?:-[>-]|<-))[^;:.\\n]+'
    <key-part-last> = <s> #'" key-reg "'
    at-key = (at-part period)* at-part-last
    <at-part> = key-part | d2-keyword
    <at-part-last> = d2-keyword
    ekey = !hash (ekey-part period)* ekey-part-last
    ekey-part = #'^[^;:.\\n-<]+'
    ekey-part-last = <s> #'" ekey-reg "'

    sh = key colon label-plus

    (* labels *)
    <label-plus> = label | block | typescript
    label = <s> #'^(?!^\\s*$)[^;{}\\n]+'
    <colon-label> = (<colon> <s> | <colon> label)
    <colon-label-plus> = (<colon> <s> | <colon> label-plus)
    block = '|' #'[^|]+' '|'
    typescript = ts-open #'(.*(?!(\\|\\|\\||`\\|)))' ts-close
    ts-open = '|||' | '|`'; ts-close = '|||' | '`|'
 
    (* building blocks *)
    <any> = #'.'
    <any-key> = #'[^.:;{\\n]'
    <sep> = #'\\r?\\n'
    <at-sep> = sep | semi
    colon = ':'
    <semi> = ';'
    <hash> = '#'
    curlyo = '{'
    curlyc = '}'
    <period> = '.'
    s = #' *'
    <d2-keyword> =" (at/d2-keys)))


(defn- line-by-line
  [a b]
  (dorun (map-indexed
      (fn [index item] (let [same? (= (nth b index) item)]
                         (if (not same?)
                           (do (println "-- Mismtach in form " index "-----")
                               (println "-- first:")
                               (println item)
                               (println "-- second:")
                               (println (nth b index))
                               (println "--------------------------------")))))
      a))
  nil)


(defn dict [d2]
  (insta/transform
   {:label (fn [& parts] (str/join parts))
    :key (fn [& parts] (str/join parts))}
   d2))

(defn- preprocess
  [d2]
  (-> d2
      ;; remove edge continuations
      (str/replace #"--\\[\\s]*\n" "")
      ;; remove trailing colons.
      ;; Getting this out in the parse would have meant abandonning
      ;; :auto-whitespace
      (str/replace #":[ ]*\n" "\n")))


(defn dictim
  "Converts a d2 string to its dictim representation.
   Each dictim element returned's type is captured in the :tag key
   of the element's metadata.
   Three optional functions may be supplied:
     :key-fn     a modifier applied to each key.
     :label-fn   a modifier applied to each label.
     :reduce-fn  a reduction function of two arguments, acc & cur which
   is applied over all elements as a last pass. This is useful, for
   example, to 'flatten' out elements captured inside of a list:
      ````
      (dictim (slurp \"in.d2\")
              :reduce-fn
              (fn [acc cur]
                (if (= :list (-> cur meta :tag))
                  (vec (concat acc (rest cur)))
                  (conj acc cur)))
              :key-fn keyword)
      ````"
  [s & {:keys [key-fn label-fn reduce-fn]
        :or   {key-fn identity
               label-fn str/trim
               reduce-fn nil}}]
  (let [p-trees (-> s preprocess p-d2)
        key-fn (comp key-fn str/trim)
        contents (fn [tag & parts] (with-meta (vec parts) {:tag tag}))]
    (if (insta/failure? p-trees)
      (throw (error (str "Parse error at: " (-> p-trees last second))))
      (mapcat
       (fn [p-tree]
         (insta/transform
          {:comment (fn [c] (with-meta [:comment (str/triml c)] {:tag :comment}))
           :key key-fn
           :label-text label-fn
           :label label-fn
           :block-text (fn [t] (str "|" t "|"))
           :dir identity
           :d2-key identity
           :d2-word (fn [& parts] (key-fn (str/join parts)))
           :in-attr-label identity
           :attr (fn
                   ([k v] (with-meta {k v} {:tag :attrs}))
                   ([k lbl m] ;; in-attr-label
                    (with-meta {k (assoc m (key-fn "label") (str/trim lbl))} {:tag :attrs})))
           :attrs (fn [& attrs] (with-meta (into {} attrs) {:tag :attrs}))
           :ts-open identity
           :ts-close (fn [c] (str "\n" c))
           :typescript (fn [& parts] (str/join parts))
           :ctr (partial contents :ctr)
           :conn (partial contents :conn)
           :shape (partial contents :shape)
           :list (fn [& elems] (with-meta (into [:list] elems) {:tag :list}))
           :elements (fn [& elems]
                       (if reduce-fn
                         (reduce reduce-fn [] elems)
                         (vec elems)))}
          p-tree))
       p-trees))))
