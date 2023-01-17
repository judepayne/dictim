(ns dictim.parse2
  {:author "judepayne"
   :doc "Namespace for parsing d2 and returning dictim format."}
  (:require [clojure.string :as str]
            [instaparse.core :as insta]
            [dictim.attributes :as at]
            [dictim.utils :refer [error]]))


(def ^:private parse-d2
  "a parser for d2"
  (insta/parser
   (str
    "<D2> = elements
    elements = (element sep)* element sep?
    <element> = list / elem    (* force list to be recognized first *)

    list = (elem <';'>)+ elem
    <elem> = comment | attr | conn | shape | ctr

    comment = hash comment-text

    attrs = <'{'> (attr sep)* attr <sep?> <'}'>
    attr = d2-word <':'> (label-text | in-attr-label? attrs)
    d2-word = (d2-key '.')* d2-key
    in-attr-label = text

    shape = !hash key opts

    conn = single | multi
    <single> = key dir key opts
    <multi> = edge+ key opts
    <edge> = key dir
    dir = '--' | '->' | '<-' | '<->'

    ctr = !hash key maybe-label <'{'> element+ <'}'>
    
    <opts> = maybe-label attrs?
    <maybe-label> = (<':'> / <':'> label)? (* choice to stop whitespace label *)
    <hash> = <'#'>
    key = text
    label = label-text | block-text | typescript
    <sep> =  <#';*\\r?\\n'>

    (* keys - must avoid seps and dirs *)
    <text> = #'([^:;\n{}|](?!(->|--|<-|<->)))+'
    (* labels - same to keys but colons are allowed *)
    label-text =  #'([^;\n{}|](?!(->|--|<-|<->)))+'
    (* comments are terminated by newlines always, so can take in anything else *)
    <comment-text> = #'.+'
    (* blocks must avoid delimiter pipe *)
    block-text = <'|'> #'[^|]+' <'|'>
    (* ditto typescript with triple pipe *)
    typescript = ts-open #'([\\s\\S](?!(\\|\\|\\||`\\|)))+' ts-close
    ts-open = '|||' | '|`'
    ts-close = '|||' | '`|'
    any = #'.+'

    d2-key = "
    (at/d2-keys))
   :auto-whitespace :standard))


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
