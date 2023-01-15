(ns dictim.parse
  {:author "judepayne"
   :doc "Namespace for parsing d2 and returning dictim format."}
  (:require [clojure.string :as str]
            [instaparse.core :as insta]
            [dictim.attributes :as at]
            [dictim.utils :refer [error]]))


(def ^:private whitespace
  "a parser for whitespace"
  (insta/parser
   "whitespace = #'[ \t]+'"))  ;; don't absorb line endings


(def ^:priavte permitted-chars
  "a-zA-Z0-9 -_$£@\\[\\]+\\'`~\"()?!,*\\\\/<>")


(def ^:private p-d2
  "a parser for d2"
  (insta/parser
   (str
    "<D2> = elements
    <elements> = <sep?> (element <sep>)* element <sep?>
    <element> = list | elem

    sep = #'\\r?\\n'+; colon = ':'; hash = '#'

    list = (elem <';'>)+ elem
    <elem> = conn | shape | comment | attr | ctr

    conn = (key dir)+ key <colon?> !hash label? attrs?
    dir = <contd?> direction
    contd = #'--\\\\\n'
    <direction> = '--' | '->' | '<-' | '<->'

    shape = key <colon?> !hash label-plus? attrs?

    comment = <hash> label

    attrs = <'{'> <sep?> (attr <sep?>)* attr <sep?> <'}'>
    attr = d2-word <':'> (val | in-attr-label? attrs)
    d2-word = (label '.')* d2-key
    in-attr-label = label
    val = label

   (* ctr attrs are inside { } *)
   ctr = key <colon?> !hash label? <'{'> elements <'}'>

    label-plus = label | block | typescript
    block = '|' #'[^|]+' '|'
    typescript = ts-open #'(.*(?!(\\|\\|\\||`\\|)))' ts-close
    ts-open = '|||' | '|`'; ts-close = '|||' | '`|'

    key = #'[a-zA-Z0-9 ]+'
    label = #'[a-zA-Z0-9 ]+'
    
    
    d2-key = " (at/d2-keys))
   :auto-whitespace whitespace))

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
  (let [p-trees (-> s preprocess parse-d2)
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
