(ns
    ^{:author "judepayne"
      :doc "Namespace for handling dictim templates"}
    dictim.templates
  (:require [dictim.utils :as utils :refer [elem-type kstr? direction? take-til-last]]
            [clojure.walk :as walk])
  (:refer-clojure :exclude [key keys test]))


;; This ns provides functions to handle the data part of a dictim diagram spec separately from
;; the styling/ rendering instructions part of the spec.
;; 1. ability to supply a fn that is applied during a pre-walk
;; 2. ability to merge a classes statement into a dictim spec
;; 3. de-merge both of the above.



;; *****************************************
;; *     accessors for dictim elements     *
;; *****************************************

;; extract the key
(defmulti key elem-type)

(defmethod key :shape [elem] (first elem))
(defmethod key :ctr [elem] (first elem))
(defmethod key :conn [elem] nil)
(defmethod key :conn-ref [elem] nil)


;; extract the label
(defmulti label elem-type)

(defmethod label :shape [elem] (when (kstr? (second elem)) (second elem)))
(defmethod label :ctr [elem] (when (kstr? (second elem)) (second elem)))
(defmethod label :conn-ref [elem] (when (map? (last elem)) (last elem)))
(defmethod label :conn [elem]
  (let [[fs ls] (take-til-last direction? elem)
        maybe-label (first (rest ls))]
    (when (kstr? maybe-label) maybe-label)))


;; extract attrs
(defn attrs [elem] (->> elem (filter map?) first))


;; extract contained (container contents)
(defmulti contained elem-type)

(defmethod contained :shape [elem] nil)
(defmethod contained :ctr [elem] (last elem))
(defmethod contained :conn [elem] nil)
(defmethod contained :conn-ref [elem] nil)


;; extract keys (only for connections)
(defmulti keys elem-type)

(defn- extract-keys [conn]
  (let [[fs [lk & _]] (take-til-last direction? conn)]
    (conj (filterv (complement direction?) fs) lk)))

(defmethod keys :shape [elem] nil)
(defmethod keys :ctr [elem] nil)
(defmethod keys :conn [elem] (extract-keys elem))
(defmethod keys :conn-ref [elem] (extract-keys elem))



(def ^{:private true}
  accessors
  {"key" key
   "label" label
   "attrs" attrs
   "contained" contained
   "keys" keys})


;; *****************************************
;; *          setter for attrs             *
;; *****************************************

(defmulti set-attrs (fn [elem _] (elem-type elem)))

(defn- set-attrs-elem [elem attrs]
  (let [proto (filterv (complement map?) elem)]
    (conj proto attrs)))

(defmethod set-attrs :shape [elem attrs] (set-attrs-elem elem attrs))
(defmethod set-attrs :conn [elem attrs] (set-attrs-elem elem attrs))
(defmethod set-attrs :conn-ref [elem attrs] (set-attrs-elem elem attrs))
(defmethod set-attrs :ctr [elem attrs]
  (let [shape-part (into [] (butlast elem))
        modified (set-attrs-elem shape-part attrs)]
    (conj modified (last elem))))


;; *****************************************
;; *                tests                  *
;; *****************************************

;; allows a test to be specified as data. Useful in over-the-wire scenarios

(def ^{:private true} comparators
  {"=" =
   "!=" not=
   "contains" some
   "doesnt-contains" (complement some)
   ">" >
   "<" <
   "<=" <=
   ">=" >=})

(defn- contains-vectors?
  "Returns true if coll contains one or more vectors."
  [coll]
  (some vector? coll))


(defmacro ^{:private true} single-test
  "Returns code that tests whether the test is true for the item
   specified by sym."
  [sym test]
  `(let [[comparator# func# v#] ~test
         v-found# ((read-string func#) ~sym)
         comp# (get comparators comparator#)]

     (cond
       (and (not (coll? v-found#))
            (or (= :contains comparator#) (= :doesnt-contain comparator#)))
       (throw (Exception. (str ":contains and :doesnt-contain can only be used on collections.")))

       (coll? v-found#)
       (comp# (conj #{} v#) v-found#)

       :else
       (comp# v-found# v#))))


(defn- single-test? [t]
  (println t (type t))
  (if
      (and
       (sequential? t)
       (get comparators (first t))
       #_(get accessors (second t)))
      true
      false))


(defn- nested-test? [nt]
  (and
   (sequential? nt)
   (contains? #{:or "or" :and "and"} (first nt))
   (every? (fn [t] (or (single-test? t) (nested-test? t))) (rest nt))))


(defn- valid-test? [t]
  (or (single-test? t) (nested-test? t)))


(def elem {:a 2})
(def tests ["and" ["=" ":a" 2] ["=" ":a" 2] ["and" ["=" ":a" 2] ["=" ":a" 2]] ["=" ":a" 2]])


(defn and* [terms]
 (reduce (fn [acc cur] (and acc cur)) terms))


(defn or* [terms]
  (reduce (fn [acc cur] (or acc cur)) terms))


(defmacro ^{:private true} dotest*
  "Takes a sym(bol) to takes and a test in the form of nested and/ or clauses e.g.:
     [\"or\" [\"=\" \":a\" 2] [\"and\" [\"=\" \":a\" 2] [\"=\" \":a\" 2]]]
   and returns code that converts the test into a function that tests whether the test
   is true for the value or var specified by sym."
  [sym test]
  (if (single-test? test)
    `(single-test ~sym ~test)
    (let [[t & ts] test]
      (println ts)
      (if ts
        (cond
          (= "and" t)
          `(and* (dotest ~sym ~ts))

          (= "or" t)
          `(or* (dotest ~sym ~ts))

          (single-test? t)
          `(cons (single-test ~sym ~t) (dotest ~sym ~ts)))
        `(list (single-test ~sym ~t))))))


(defmacro ^{:private true} dotest2
  "Takes a sym(bol) to takes and a test in the form of nested and/ or clauses e.g.:
     [\"or\" [\"=\" \":a\" 2] [\"and\" [\"=\" \":a\" 2] [\"=\" \":a\" 2]]]
   and returns code that converts the test into a function that tests whether the test
   is true for the value or var specified by sym."
  [sym test & tests]
  
  (println "outside code " "t " test " ts: " tests)
  (println "              ts nil?: " (nil? tests) " single-test?: " (single-test? test))

  (if tests
    `(do (println "here" " test: "~test "tests: " (list ~@tests))
         (cond
           (= "and" ~test)
           (and* (dotest2 ~sym ~@tests))

           (= "or" ~test)
           (or* (dotest2 ~sym ~@tests))

           (single-test? ~test)
           (cons (single-test ~sym ~test) (dotest2 ~sym ~@tests))))
    `(do (println "there")
       (if (single-test? ~test)
         (list (single-test ~sym ~test))
         (let [t# (first ~test)
               ts# (rest ~test)]
           (cond
             (= "and" (first ~test))
             (and* (dotest2 ~sym ~@(rest test)))

             ))))))




#_(let [[t# & ts#] ~test]
    (throw (Exception. (str t# " " ts#  " must end with a test"))))


;; ***************


(defn- principal-elem? [form]
  (contains? #{:shape :ctr :conn :conn-ref} (elem-type form)))


(defn- add-attrs
  "Decorates a piece of dictim with attrs. f is a function that takes
   an elem and returns the attrs to be added to the elem (or nil)."
  [f dict]
  (let [edit-fn (fn [form]
                  (if (principal-elem? form)
                    (let [attrs (f form)]
                      (if attrs
                        (set-attrs form attrs)
                        form))
                    form))]
    (walk/postwalk edit-fn dict)))









(defn remove-maps
  "removes all maps from the nested form."
  [dict]
  (let [remove-fn (fn [form]
                    (cond
                      (map? form)           nil

                      (principal-elem? form)
                      (into [] (remove map? form))

                      :else form))]
    (remove nil? (walk/prewalk remove-fn dict))))
