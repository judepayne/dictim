(ns
    ^{:author "judepayne"
      :doc "Namespace for handling dictim templates"}
    dictim.template
  (:require [dictim.utils :as utils :refer [elem-type kstr? direction? take-til-last]]
            [clojure.walk :as walk])
  (:refer-clojure :exclude [key keys test]))


;; This ns provides functions to handle the data part of a dictim diagram spec separately from
;; the styling/ rendering instructions part of the spec.
;; See the two public api functions at the bottom.

;; *****************************************
;; *     accessors for dictim elements     *
;; *****************************************

;; extract the key
(defmulti key "Returns the key of a dictim element" elem-type)

(defmethod key :shape [elem] (first elem))
(defmethod key :ctr [elem] (first elem))
(defmethod key :conn [elem] nil)
(defmethod key :conn-ref [elem] nil)


;; extract the label
(defmulti label "Returns the label of a dictim element" elem-type)

(defmethod label :shape [elem] (when (kstr? (second elem)) (second elem)))
(defmethod label :ctr [elem] (when (kstr? (second elem)) (second elem)))
(defmethod label :conn-ref [elem] (when (map? (last elem)) (last elem)))
(defmethod label :conn [elem]
  (let [[fs ls] (take-til-last direction? elem)
        maybe-label (first (rest ls))]
    (when (kstr? maybe-label) maybe-label)))


;; extract attrs
(defn- attrs
  "Returns the attrs of a dictim element"
  [elem] (->> elem (filter map?) first))


;; extract contained (container contents)
(defmulti children "Returns the children elements of a dictim element" elem-type)

(defmethod children :shape [elem] nil)
(defmethod children :ctr [elem] (filter vector? elem))
(defmethod children :conn [elem] nil)
(defmethod children :conn-ref [elem] nil)
(defmethod children :elements [elem] elem)


;; extract keys (only for connections)
(defmulti keys "returns the keys of a dictim element" elem-type)

(defn- extract-keys [conn]
  (let [[fs [lk & _]] (take-til-last direction? conn)]
    (conj (filterv (complement direction?) fs) lk)))

(defmethod keys :shape [elem] nil)
(defmethod keys :ctr [elem] nil)
(defmethod keys :conn [elem] (extract-keys elem))
(defmethod keys :conn-ref [elem] (extract-keys elem))


;; extract the element type
(defn element-type
  "Returns the dictim element type as a string. Either:
   shape, conn, conn-ref, ctr, attrs, quikshape, cmt, empty-lines, list, elements or nil"
  [elem]
  (name (elem-type elem)))


(def ^{:private true}
  accessors
  {"key" dictim.template/key
   "label" dictim.template/label
   "attrs" dictim.template/attrs
   "children" dictim.template/children
   "keys" dictim.template/keys
   "type" dictim.template/element-type})



;; *****************************************
;; *          setter for attrs             *
;; *****************************************

(defmulti ^:private set-attrs! (fn [elem _] (elem-type elem)))

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


(defn- single-test? [t]
  (and
   (sequential? t)
   (get comparators (first t))
   (get accessors (second t))))


(defn- nested-test? [nt]
  (and
   (sequential? nt)
   (contains? #{:or "or" :and "and"} (first nt))
   (every? (fn [t] (or (single-test? t) (nested-test? t))) (rest nt))))


(defn- valid-test? [t]
  (or (single-test? t) (nested-test? t)))


(def ^:dynamic ^:private *elem*)


(defn- single-test
  "Returns code that tests whether the test is true for the item
   specified by sym."
  [test]
  (let [[comparator func v] test
        f (get accessors func)
        v-found (f *elem*)
        comp (get comparators comparator (constantly nil))]

    (cond
      (and (not (coll? v-found))
           (or (= :contains comparator) (= :doesnt-contain comparator)))
      (throw (Exception. (str ":contains and :doesnt-contain can only be used on collections.")))

      (coll? v-found)
      (comp (conj #{} v) v-found)

      :else
      (comp v-found v))))


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
    
    (single-test? tests)
    (single-test tests)

    :else (throw (Exception. "Not a valid test."))))


(defn- test
  "Checks that that *elem* conforms to the tests."
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


(defn- template-fn
  [styles]
  (fn [element]
    (binding [dictim.template/*elem* element]
      (styles* styles))))


;; *****************************************
;; *              public api               *
;; *****************************************


(defn- principal-elem? [form]
  (contains? #{:shape :ctr :conn :conn-ref} (elem-type form)))


(defn walk-dictim
  "Similar to clojure.walk/walk but refined for dictim. Only walks into
   children of elements."
  [inner outer element]
  (case (elem-type element)
    :elements (outer (apply list (map inner element)))
    :ctr (let [[nc c] (split-with (complement vector?) element)]
           (outer (apply conj (into [] nc)
                         (mapv inner c))))
    (outer element)))


(defn postwalk-dictim
  "Similar to clojure.walk/postwalk but for dictim."
  [f element]
  (walk-dictim (partial postwalk-dictim f) f element))


(defn prewalk-dictim
  "Similar to clojure.walk/prewalk but for dictim."
  [f element]
  (walk-dictim (partial prewalk-dictim f) identity (f element)))


(defn- combine-directives
  [old new]
  (let [comb (merge old new)]
    (seq (reduce (fn [a [k v]] (conj a {k v})) [] comb))))


(defn add-styles
  "Walks the supplied dictim edn and decorates with attrs and top level directives.
   template can be either:
     - a function that takes an elem and returns the attrs to be added
       to the elem (or nil).
     - a sequence of test and attribute pairs to be applied if the test succeeds.
       A simple test has the form [<comparator> <accessor> <value>] where:
       - <comparator> is a string, one of the `comparators` var's keys in
         this namespace.
       - <accessor> is a string, one of the `accessors` var's keys in this
         namespace. Accessors allow you to access values of a dictim element,
         for example its `key` or `label`.
       - <value> is the value to be tested against.
         Example simple test: `[\"=\" \"key\" \"node123\"]`
       A nested test nests simple tests with `and` and `or` statements.
         Example nested test:
         `[\"and\" [\"=\" \"type\" \"ctr\"] [\"=\" \"key\" \"node123\"]]`
       Attributes are supplied using standard dictim, e.g. `{:style.fill \"red\"}`
   directives is a map of attrs to be added at the top level e.g. `{\"classes\"...}`
   If there are directives in the original dict, the new directives will be merge over them."
  ([dict template] (add-styles dict template nil))
  ([dict template directives]
   (assert (or (nil? directives) (map? directives)))
   (let [attrs-fn (if (fn? template)
                    template
                    (template-fn template))
         edit-fn (fn [form]
                   (if (principal-elem? form)
                     (let [attrs (attrs-fn form)]
                       (if attrs
                         (set-attrs! form attrs)
                         form))
                     form))
         old-dirs (reduce merge (filter map? dict))
         data-elements (into '() (remove map? dict))
         new-dirs (combine-directives old-dirs directives)
         walked (postwalk-dictim edit-fn data-elements)]
     (if new-dirs
       (if (list? walked)
         (concat new-dirs walked)
         (concat new-dirs (list walked)))
       walked))))


(defn remove-styles
  "removes all maps from the nested form. i.e. attrs and directives.
   If retain-vars? is true, :vars/\"vars\" attributes will be retained in the dictim,
   since vars can be part of the 'data side' of a piece of dictim."
  [dict & {:keys [retain-vars?] :or {retain-vars? false}}]
  (let [remove-fn (fn [form]
                    (cond
                      (map? form)           (if retain-vars?
                                              (let [vars (select-keys form [:vars "vars"])]
                                                (if (empty? vars)
                                                  nil
                                                  vars))
                                              nil)

                      (principal-elem? form)
                      (into [] (remove map? form))

                      :else form))]
    (remove nil? (prewalk-dictim remove-fn dict))))
