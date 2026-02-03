(ns
    ^{:author "judepayne"}
    dictim.tests
  (:require [dictim.utils :as utils :refer [elem-type kstr? direction?
                                            take-til-last error deep-merge]])
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
(defmethod key :conn [elem] (let [[kp r] (take-til-last direction? elem)]
                              (conj (into [] kp) (first r))))
(defmethod key :conn-ref [elem] (into [] (take 3 elem)))
(defmethod key :elements [_] nil)
(defmethod key :else [_] nil)


;; extract the label
(defmulti label "Returns the label of a dictim element" -elem-type)

(defmethod label :shape [elem] (when (kstr? (second elem)) (second elem)))
(defmethod label :ctr [elem] (when (kstr? (second elem)) (second elem)))
(defmethod label :conn-ref [elem] (when (map? (last elem)) (last elem)))
(defmethod label :conn [elem]
  (let [[_ ls] (take-til-last direction? elem)
        maybe-label (first (rest ls))]
    (when (kstr? maybe-label) maybe-label)))
(defmethod label :elements [_] nil)
(defmethod label :else [_] nil)


;; extract attrs
(defn attrs
  "Returns the attrs of a dictim element"
  [elem] (->> elem (filter map?) first))


;; extract contained (container contents)
(defmulti children "Returns the children elements of a dictim element" -elem-type)

(defmethod children :shape [_] nil)
(defmethod children :ctr [elem] (filter vector? elem))
(defmethod children :conn [_] nil)
(defmethod children :conn-ref [_] nil)
(defmethod children :elements [elem] elem)
(defmethod children :else [_] nil)


;; extract keys (only for connections)
(defmulti keys "returns the keys of a dictim element" -elem-type)

(defn- extract-keys [conn]
  (let [[fs [lk & _]] (take-til-last direction? conn)]
    (seq (conj (filterv (complement direction?) fs) lk))))

(defmethod keys :shape [_] nil)
(defmethod keys :ctr [_] nil)
(defmethod keys :conn [elem] (extract-keys elem))
(defmethod keys :conn-ref [elem] (extract-keys elem))
(defmethod keys :elements [_] nil)
(defmethod keys :else [_] nil)


;; extract the element type
(defn element-type
  "Returns the dictim element type as a string. Either:
   shape, conn, conn-ref, ctr, attrs, quikshape, cmt, empty-lines, list, elements or nil"
  [elem]
  (name (elem-type elem)))


(def ^{:private true}
  accessors
  {"key" dictim.tests/key
   "label" dictim.tests/label
   "attrs" dictim.tests/attrs
   "children" dictim.tests/children
   "keys" dictim.tests/keys
   "element-type" dictim.tests/element-type})


;; *****************************************
;; *                setters                *
;; *****************************************

(defmulti set-attrs!
  "Sets the attrs of a dictim element"
  (fn [elem _] (-elem-type elem)))

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


(defmulti set-label!
  "Sets the attrs of a dictim element"
  (fn [elem _] (-elem-type elem)))

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
(def ^{:private true
       :dynamic true
       :doc "Implementation detail exposed for testing purposes only!"}
  *elem*)


(defn- matches-test? [t]
  (and
   (sequential? t)
   (= "matches" (first t))
   #_(boolean (get accessors (second t)))
   (try (re-pattern (nth t 2))
          true
          (catch Exception _ false))))


(def ^{:private true} comparators
  {"=" =
   "!=" not=
   "contains" some
   "doesnt-contain" (complement some)
   ">" >
   "<" <
   "<=" <=
   ">=" >=})


(defn- comparison-test? [t]
  (and
   (sequential? t)
   (boolean (get comparators (first t)))
   #_(boolean (get accessors (second t)))))


(defn- nested-test? [nt]
  (and
   (sequential? nt)
   (contains? #{:or "or" :and "and"} (first nt))
   (every? (fn [t] (or :else "else" (matches-test? t) (comparison-test? t) (nested-test? t))) (rest nt))))


(defn valid-test?
  "Returns true if a data-form test is valid. A test may be simple, i.e. in the form
   [<comparator> <accessor> <value>] e.g. [\"=\" \"key\" \"Steve\"]
   or use the \"and\" \"or\" keywords to arbitrarily nest simple tests. e.g.
   [\"and\" [\"=\" \"element-type\" \"shape\"]
             [\"or\" [\"=\" \"key\" \"app14181\"] [\"=\" \"key\" \"app14027\"]]]"
  [t]
  (or (= t :else) (= t "else") (matches-test? t) (comparison-test? t) (nested-test? t)))


(defn- matches-test
  "tests whether the matches test is true for *elem*
   *elem* may be either a dictim element or a map."
  [test]
  (let [[_ accessor reg-str] test
        r (re-pattern reg-str)
        v-found (if-let [f (get accessors accessor)]  ;; dictim element
                  (f *elem*)
                  (if (vector? accessor)
                    (get-in *elem* accessor) ;; assume a nested map
                    (get *elem* accessor)))] ;; assume a map
    (if (string? v-found)
      (re-matches r v-found)
      false)))


(defn- comparison-test
  "tests whether the comparison test is true for *elem*
   *elem* may be either a dictim element or a map."
  [test]
  (let [[comparator accessor v] test
        v-found (if-let [f (get accessors accessor)] ;; dictim element
                  (f *elem*)
                  (if (vector? accessor)
                    (get-in *elem* accessor) ;; assume a nested map                    
                    (get *elem* accessor)))  ;; assume a map
        comp (get comparators comparator (constantly nil))]
    (cond
      (contains? #{"contains" "doesnt-contain"} comparator)
      (cond
        (sequential? v-found)
        (comp #(= v %) v-found)
        
        (and (map? v-found) (map? v))
        (let [contains-fn (fn [found-map test-map]
                            (every? (fn [[k v]] (= (get found-map k) v)) test-map))]
          (if (= comparator "contains")
            (contains-fn v-found v)
            (not (contains-fn v-found v))))
        
        (nil? v-found)
        (= comparator "doesnt-contain") ; nil doesn't contain anything
        
        :else
        (throw (error (str "contains/ doesnt-contain can only be used on sequential items or maps"
                           " in test: " test ". Failed on element: " *elem*))))
      
      :else (if (nil? v-found)
              (= comparator "!=")
              (try
                (comp v-found v)
                (catch Exception ex
                  (throw (error (str "This test " test " error'd when applied to this element: "
                                     *elem*)))))))))


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


(defn- test-impl
  [tests]
  (cond
    (nested-test? tests)
    (cond
      (= "and" (first tests))
      (and* (map #(test-impl %) (rest tests)))

      (= "or" (first tests))
      (or* (map #(test-impl %) (rest tests))))
    
    (comparison-test? tests)
    (comparison-test tests)

    (matches-test? tests)
    (matches-test tests)

    (or (contains? #{:else "else" "_"} tests))
    true

    :else (throw (Exception. "Not a valid test."))))


(defn- test
  [tests]
  (assert (valid-test? tests)
          (str tests " is not a valid test."))
  (test-impl tests))


(defn test-elem
  "Checks that that elem conforms to the tests."
  [tests elem]
  (binding [*elem* elem]
    (test tests)))


(defn- styles*
  [styles]
  (when styles
    (if (test (first styles))   ;; use the private -test function
      (if (next styles)
        (second styles)
        (throw (IllegalArgumentException. "test clauses must be an even number")))
      (styles* (next (next styles))))))


(defn- reverse2 [coll]
  (->> coll (partition 2) reverse (mapcat identity) vec))


(defn- merge-results
  ([styles] (merge-results nil styles))
  ([result styles]
   (when styles
     (if (test (first styles))
       (if (next styles)
         (deep-merge result (second styles) (merge-results (next (next styles))))
         (throw (IllegalArgumentException. "test clauses must be an even number")))
       (deep-merge result (merge-results (next (next styles))))))))


(defn test-fn
  "When passed a sequence of <test> <value> pairs, returns a 1-arity function
   that evaluates each test against the argument passed to the function and
   returns the value associated with the first true test. Either `:else` or `\"else\"`
   can be used in the second last position to create a catch all clause in the
   function returned."
  [tests]
  (fn [element]
    (binding [*elem* element]
      (styles* tests))))


(defn test-fn-merge
  "When passed a sequence of <test> <value> pairs, returns a 1-arity function
   that evaluates each test against the argument passed to the function and
   returns the value associated by merging the values of all true test.
   Either `:else` or `\"else\"` can be used in the second last position to
   create a catch all clause in the function returned."
  [tests]
  (fn [element]
    (binding [*elem* element]
      (merge-results (reverse2 tests)))))
