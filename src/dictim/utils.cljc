(ns ^{:author "judepayne"
      :doc "Namespace for common functions."}
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


(defn conn-ref-ptr?
  [s]
  (and (vector? s)
       (= 1 (count s))
       (or (integer? (first s))
           (= "*" (first s)))))


(defn take-til-last
  "take until the last match. not every item needs match"
  [pred coll]
  (-> (reduce
       (fn [acc cur]
         (cond
           (nil? cur)    (:banked acc)
             
           (pred cur)    (-> acc
                             (update :banked concat (conj (:buffer acc) cur))
                             (assoc-in [:buffer] []))
             
           :else         (update acc :buffer conj cur)))
       {:banked [] :buffer []}
       coll)
      vals))


(defn conn-ref?
  "Returns true if element is a connection reference style attribute key."
  [e]
  (and (vector? e)
       (direction? (second e))
       (= (count e) 4)
       (conn-ref-ptr? (nth e 3))))


;; element types

(defn elem-type
  "Returns the type of dictim element e."
  [e]
  (cond
    (= :empty-lines (first e))         :empty-lines
    (map? e)                           :attrs
    (kstr? e)                          :quikshape
    (= :comment (first e))             :cmt
    (= :list (first e))                :list
    (not (empty? (filter vector? e)))  :ctr
    (direction? (second e))            :conn
    :else                              :shape))


(defn ctr? [e] (= :ctr (elem-type e)))


(defn attrs? [e] (= :attrs (elem-type e)))


(defn conn? [e] (= :conn (elem-type e)))


(defn shape? [e] (= :shape (elem-type e)))


(defn cmt? [e] (= :cmt (elem-type e)))


(defn list? [e] (= :list (elem-type e)))


(defn- parse-int [s]
  #?(:clj
     (try
       (let [n (Integer/parseInt s)]
         n)
       (catch Exception e nil))
     :cljs
     (let [n (js/parseInt s 10)]
       (if (js/Number.isNan n)
         nil
         n))))


(defn parse-float [s]
  #?(:clj
     (try
       (let [f (Float/parseFloat s)]
         f)
       (catch Exception e nil))
     :cljs
     (let [f (js/parseFloat s 10)]
       (if (js/Number.isNan f)
         nil
         n))))


(defn- parse-bool [s]
  (let [s' (str/trim s)]
    (if (or (= "true" s') (= "false" s'))
      #?(:clj (clojure.edn/read-string s')
         :cljs (cljs.reader/read-string s'))
      nil)))


(defn try-parse-primitive [p]
  (if-let [i (parse-int p)]
    i
    (if-let [f (parse-float p)]
      f
      (let [b (parse-bool p)]
        (if (nil? b)
          p
          b)))))


(defn prn-repl
  "Prints multiline strings nicely in the repl"
  [d2]
  (run! println (str/split d2 #"\\n")))


(defn line-by-line
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


;; This regexes match a period but only when not inside single quotes
;; https://stackoverflow.com/questions/6462578/regex-to-match-all-instances-not-inside-quotes

(def unquoted-period #"\.(?=([^']*'[^']*')*[^']*$)")


;; matches when starts and ends with '
(def single-quoted #"^'.*'$")

;; does no
(def no-asterisk #"^[^*]*$")


(defn convert-key [k]
  (cond
    (number? k) (str k)
    (keyword? k) (name k)
    :else k))
