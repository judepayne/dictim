(ns ^{:author "judepayne"
      :doc "Namespace for common dictim utility functions."}
    dictim.utils
  (:refer-clojure :exclude [list?])
  (:require [clojure.string :as str]))


(defn error
  "Creates an exception object with error-string."
  [error-string]
  #?(:clj (Exception. ^String error-string)
     :cljs (js/Error. error-string)))


(defn kstr?
  "Value is either a keyword or a string?"
  [v]
  (or (number? v) (keyword? v) (string? v)))


(defn direction?
  [s]
  (contains? #{"--" "<->" "->" "<-"} s))


(defn take-til-last
  "take until the last match. not every item needs match"
  [pred coll]
  (-> (reduce
       (fn [acc cur]
         (cond
           #_(nil? cur)    #_(:banked acc)
             
           (pred cur)    (-> acc
                             (update :banked concat (conj (:buffer acc) cur))
                             (assoc-in [:buffer] []))
             
           :else         (update acc :buffer conj cur)))
       {:banked [] :buffer []}
       coll)
      vals))


(defn- conn-ref-ptr?
  [s]
  (and (vector? s)
       (= 1 (count s))
       (or (integer? (first s))
           (= "*" (first s)))))


(defn- conn-ref?*
  "Returns true if element is a connection reference style attribute key."
  [e]
  (and (vector? e)
       (direction? (second e))
       (= (count e) 5)
       (conn-ref-ptr? (nth e 3))))


;; element types

(defn elem-type
  "Returns the type of dictim element e."
  [e]
  (cond
    (nil? e)                                    :nil
    (conn-ref?* e)                              :conn-ref
    (and (vector? e)
         (or (= :empty-lines (first e))
             (= "empty-lines" (first e))))      :empty-lines
    (map? e)                                    :attrs
    (kstr? e)                                   :quikshape
    (and (vector? e)
         (or (= :comment (first e))
             (= "comment" (first e))))          :cmt
    (and (vector? e)
         (or (= :list (first e))
             (= "list" (first e))))             :list
    (and (vector? e)
         (seq (filter vector? e)))              :ctr
    (and (vector? e)
         (direction? (second e)))               :conn
    (not (clojure.core/list? e))                :shape
    (clojure.core/list? e)                      :elements
    :else
    (throw (error "element type not recognized"))))


(defn ctr?
  "Returns true if the element is a container"
  [e] (= :ctr (elem-type e)))


(defn attrs?
  "Returns true if the element is attrs"
  [e] (= :attrs (elem-type e)))


(defn conn?
  "Returns true if the element is a connection"
  [e] (= :conn (elem-type e)))


(defn conn-ref?
  "Returns true if the element is a connection reference"
  [e] (= :conn-ref (elem-type e)))


(defn shape?
  "Returns true if the element is a shape"
  [e] (= :shape (elem-type e)))


(defn cmt?
  "Returns true if the element is a comment"
  [e] (= :cmt (elem-type e)))


(defn list?
  "Returns true if the element is a list"
  [e] (= :list (elem-type e)))


(defn elements?
  "Returns true if the element is a collection of elements.
   Does not perform validation."
  [e] (= :list (elem-type e)))


(defn- parse-int [s]
  #?(:clj
     (try
       (let [n (Integer/parseInt s)]
         n)
       (catch Exception _ nil))
     :cljs
     (let [n (js/parseInt s 10)]
       (if (js/Number.isNan n)
         nil
         n))))


(defn- parse-float [s]
  #?(:clj
     (try
       (let [f (Float/parseFloat s)]
         f)
       (catch Exception _ nil))
     :cljs
     (let [f (js/parseFloat s 10)]
       (if (js/Number.isNan f)
         nil
         f))))


(defn- parse-bool [s]
  (let [s' (str/trim s)]
    (if (or (= "true" s') (= "false" s'))
      #?(:clj (clojure.edn/read-string s')
         :cljs (cljs.reader/read-string s'))
      nil)))


(defn- try-parse-primitive* [p]
  (try
    (if-let [i (parse-int p)]
      i
      (if-let [f (parse-float p)]
        f
        (let [b (parse-bool p)]
          (if (nil? b)
            p
            b))))
    #?(:clj (catch Exception _ p)
       :cljs (catch js/Error _ p))))


(defn try-parse-primitive
  "Attempts to parse into either a boolean, float or int.
   Returns the argument directly if the parse was unsuccessful."
  [p]
  (cond
    (sequential? p)
    (mapv try-parse-primitive* p)

    (or (float? p) (boolean? p) (integer? p))
    p

    :else (try-parse-primitive* p)))



(defn prn-repl
  "Prints multiline strings nicely in the repl"
  [d2]
  (run! println (str/split d2 #"\\n")))


(defn line-by-line
  "Compares two data structures line by line"
  [a b]
  (dorun (map-indexed
          (fn [index item] (let [same? (= (nth b index) item)]
                             (when (not same?)
                               (println "-- Mismtach in form " index "-----")
                               (println "-- first:")
                               (println item)
                               (println "-- second:")
                               (println (nth b index))
                               (println "--------------------------------"))))
          a))
  nil)


;; This regexes match a period but only when not inside single quotes
;; https://stackoverflow.com/questions/6462578/regex-to-match-all-instances-not-inside-quotes

(def unquoted-period
  "regex for a period that is not (somewhere) inside single quotes"
  #"\.(?=([^']*'[^']*')*[^']*$)")


;; unquoted period or ampersand
(def unquoted-period-or-ampersand
  "regex for an unquoted period or an ampersand"
  #"([&]|\.(?=([^']*'[^']*')*[^']*$))")


;; matches when starts and ends with '
(def single-quoted
  "regex that matches any string that is surrounding by single quotes"
  #"^'.*'$")

;; does no
(def no-asterisk
  "regex for any string that does not contain an asterisk"
  #"^[^*]*$")


(defn convert-key
  "Converts a dictim element key to a string representation."
  [k]
  (cond
    (number? k) (str k)
    (keyword? k) (name k)
    :else k))
