(ns
    ^{:author "judepayne"
      :doc "Namespace for handling dictim templates"}
    dictim.template
  (:require [dictim.utils :as utils :refer [elem-type elements?]]
            [clojure.string :as str]
            [dictim.template.impl :as impl])
  (:refer-clojure :exclude [key keys test]))

;; import accessors

(def key "Returns the key of a dictim element" impl/key)
(def keys "Returns the keys of a dictim connection/ connection reference" impl/keys)
(def label "Returns the label of a dictim element" impl/label)
(def children "Returns the children elements of a dictim element" impl/children)
(def element-type "Returns the string type of a dictim element" impl/element-type)

;; and setters

(def set-attrs! "Returns the elem modified to include/ overwrite with the supplied attribute" impl/set-attrs!)
(def set-label! "Returns the element modified to include/ overwrite with the supplied label" impl/set-label!)

(def valid-test? "Logical true if the data test supplied is valid" impl/valid-test?)

(defn test
  "Runs the data test against the supplied element.
  e.g. (test [\"=\" \"key\" :app01] [:app01 \"Solar Sky\"]) => true"
  [test element]
  (binding [impl/*elem* element]
    (impl/test test)))

(defn template-fn
  "When passed a sequence of <test> <value> pairs, returns a 1-arity function
   that evaluates each test against the argument passed to the function and
   returns the value associated with the first true test."
  [tests]
  (impl/template-fn tests))


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
       A comparison test has the form [<comparator> <accessor> <value>] where:
       - <comparator> is a string, one of the `comparators` var's keys in
         this namespace.
       - <accessor> is a string, one of the `accessors` var's keys in this
         namespace. Accessors allow you to access values of a dictim element,
         for example its `key` or `label`.
       - <value> is the value to be tested against.
         Example simple test: `[\"=\" \"key\" \"node123\"]`
       A regex matching test has the form [\"matches\" <accessor> <regex-string>] where:
       - <accessor> .. same as for comparison test
       - <regex-string> .. a string representation of a java.util.regex.Pattern
       A nested test nests comparison/ matching tests with `and` and `or` statements.
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
                    (impl/template-fn template))
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
