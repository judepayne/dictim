(ns
    ^{:author "judepayne" :no-doc true}
    dictim.template.impl
  (:require [dictim.utils :as utils :refer [elem-type kstr? direction? take-til-last]]
            [clojure.string :as str])
  (:refer-clojure :exclude [key keys test]))


;; *****************************************
;; *     accessors for dictim elements     *
;; *****************************************
(defn- -elem-type [elem]
  (let [in-scope-elems #{:shape :conn :conn-ref :ctr :elements}
        et (elem-type elem)]
    (if (contains? in-scope-elems et)
      et
      :else)))


;; extract the key
(defmulti key "Returns the key of a dictim element" -elem-type)

(defmethod key :shape [elem] (first elem))
(defmethod key :ctr [elem] (first elem))
(defmethod key :conn [elem] nil)
(defmethod key :conn-ref [elem] nil)
(defmethod key :elements [elem] nil)
(defmethod key :else [elem] nil)


;; extract the label
(defmulti label "Returns the label of a dictim element" -elem-type)

(defmethod label :shape [elem] (when (kstr? (second elem)) (second elem)))
(defmethod label :ctr [elem] (when (kstr? (second elem)) (second elem)))
(defmethod label :conn-ref [elem] (when (map? (last elem)) (last elem)))
(defmethod label :conn [elem]
  (let [[fs ls] (take-til-last direction? elem)
        maybe-label (first (rest ls))]
    (when (kstr? maybe-label) maybe-label)))
(defmethod label :elements [elem] nil)
(defmethod label :else [elem] nil)


;; extract attrs
(defn attrs
  "Returns the attrs of a dictim element"
  [elem] (->> elem (filter map?) first))


;; extract contained (container contents)
(defmulti children "Returns the children elements of a dictim element" -elem-type)

(defmethod children :shape [elem] nil)
(defmethod children :ctr [elem] (filter vector? elem))
(defmethod children :conn [elem] nil)
(defmethod children :conn-ref [elem] nil)
(defmethod children :elements [elem] elem)
(defmethod children :else [elem] nil)


;; extract keys (only for connections)
(defmulti keys "returns the keys of a dictim element" -elem-type)

(defn- extract-keys [conn]
  (let [[fs [lk & _]] (take-til-last direction? conn)]
    (seq (conj (filterv (complement direction?) fs) lk))))

(defmethod keys :shape [elem] nil)
(defmethod keys :ctr [elem] nil)
(defmethod keys :conn [elem] (extract-keys elem))
(defmethod keys :conn-ref [elem] (extract-keys elem))
(defmethod keys :elements [elem] nil)
(defmethod keys :else [elem] nil)


;; extract the element type
(defn element-type
  "Returns the dictim element type as a string. Either:
   shape, conn, conn-ref, ctr, attrs, quikshape, cmt, empty-lines, list, elements or nil"
  [elem]
  (name (elem-type elem)))


(def ^{:private true}
  accessors
  {"key" dictim.template.impl/key
   "label" dictim.template.impl/label
   "attrs" dictim.template.impl/attrs
   "children" dictim.template.impl/children
   "keys" dictim.template.impl/keys
   "element-type" dictim.template.impl/element-type})


;; *****************************************
;; *                setters                *
;; *****************************************

(defmulti set-attrs! (fn [elem _] (-elem-type elem)))

(defn- set-attrs-elem [elem attrs]
  (let [proto (filterv (complement map?) elem)]
    (conj proto attrs)))

(defmethod set-attrs! :shape [elem attrs] (set-attrs-elem elem attrs))
(defmethod set-attrs! :conn [elem attrs] (set-attrs-elem elem attrs))
(defmethod set-attrs! :conn-ref [elem attrs] (set-attrs-elem elem attrs))
(defmethod set-attrs! :ctr [elem attrs]
  (let [shape-part (into [] (butlast elem))
        modified (set-attrs-elem shape-part attrs)]
    (conj modified (last elem))))
(defmethod set-attrs! :elements [elem _] elem)
(defmethod set-attrs! :else [elem _] elem)


(defmulti set-label! (fn [elem _] (-elem-type elem)))

(defn- set-label-shp-ctr [elem label]
  (let [[pre post] (split-at 1 elem)
        elem* (if (kstr? (first post))
               (concat pre [label] (rest post))
               (concat pre [label] post))]
    (into [] elem*)))



(defmethod set-label! :shape [elem label] (set-label-shp-ctr elem label))
(defmethod set-label! :ctr [elem label] (set-label-shp-ctr elem label))
(defmethod set-label! :conn-ref [elem _] elem) ;;conn-ref's can't have labels
(defmethod set-label! :elements [elems _] elems) ;;elements can't have labels
(defmethod set-label! :else [elem _] elem)
(defmethod set-label! :conn [elem label]
  (let [[fs ls] (take-til-last direction? elem)
        lss (if (kstr? (second ls))
              (cons (first ls) (cons label (rest (rest ls))))
              (cons (first ls) (cons label (rest ls))))]
    (into [] (concat fs lss))))


;; *****************************************
;; *                tests                  *
;; *****************************************

;; allows a test to be specified as data. Useful in over-the-wire scenarios

;; This is dynamically bound to each element being tested
(def ^{:dynamic true
       :doc "Implementation detail exposed for testing purposes only!"}
  *elem*)


(defn- matches-test? [t]
  (and
   (sequential? t)
   (= "matches" (first t))
   (get accessors (second t))
   (try (re-pattern (nth t 2))
          true
          (catch Exception _ false))))


(def ^{:private true} comparators
  {"=" =
   "!=" not=

   ;; decided to hide these as not useful
   #_"contains" #_some
   #_"doesnt-contain" #_(complement some)
   #_">" #_>
   #_"<" #_<
   #_"<=" #_<=
   #_">=" #_>=})


(defn- comparison-test? [t]
  (and
   (sequential? t)
   (get comparators (first t))
   (get accessors (second t))))


(defn- nested-test? [nt]
  (and
   (sequential? nt)
   (contains? #{:or "or" :and "and"} (first nt))
   (every? (fn [t] (or (matches-test? t) (comparison-test? t) (nested-test? t))) (rest nt))))


(defn valid-test?
  "Returns true if a data-form test is valid. A test may be simple, i.e. in the form
   [<comparator> <accessor> <value>] e.g. [\"=\" \"key\" \"Steve\"]
   or use the \"and\" \"or\" keywords to arbitrarily nest simple tests. e.g.
   [\"and\" [\"=\" \"element-type\" \"shape\"]
             [\"or\" [\"=\" \"key\" \"app14181\"] [\"=\" \"key\" \"app14027\"]]]"
  [t]
  (or (matches-test? t) (comparison-test? t) (nested-test? t)))


(defn- matches-test
  [test]
  (let [[_ accessor reg-str] test
        r (re-pattern reg-str)
        f (get accessors accessor)
        v-found (f *elem*)]
    (if (string? v-found)
      (re-matches r v-found)
      false)))


(defn- comparison-test
  "Returns code that tests whether the test is true for the item
   specified by sym."
  [test]
  (let [[comparator accessor v] test
        f (get accessors accessor)
        v-found (f *elem*)
        comp (get comparators comparator (constantly nil))]    
    (comp v-found v)))


(defn- or* [forms]
  (reduce (fn [acc cur]
            (let [res (or acc cur)]
              (if res (reduced true) false)))
          forms))


(defn- and* [forms]
  (reduce (fn [acc cur]
            (let [res (and acc cur)]
              (if res true (reduced false))))
          forms))


(defn- -test
  [tests]
  (cond
    (nested-test? tests)
    (cond
      (= "and" (first tests))
      (and* (map #(-test %) (rest tests)))

      (= "or" (first tests))
      (or* (map #(-test %) (rest tests))))
    
    (comparison-test? tests)
    (comparison-test tests)

    (matches-test? tests)
    (matches-test tests)

    :else (throw (Exception. "Not a valid test."))))


(defn test
  "Checks that that *elem* conforms to the tests.
   Usage: (binding [dictim.template.impl/*elem* <your-test-elem>] (test <the-test>))"
  [tests]
  (assert (valid-test? tests)
          (str tests " is not a valid test."))
  (-test tests))


(defn- styles*
  [styles]
  (when styles
    (if (test (first styles))
      (if (next styles)
        (second styles)
        (throw (IllegalArgumentException. "styles requires an even number of forms")))
      (styles* (next (next styles))))))


(defn template-fn
  "When passed a sequence of <test> <value> pairs, returns a 1-arity function
   that evaluates each test against the argument passed to the function and
   returns the value associated with the first true test."
  [tests]
  (fn [element]
    (binding [*elem* element]
      (styles* tests))))
