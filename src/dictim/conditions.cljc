(ns
    ^{:author "judepayne"
      :doc "Conditions are a way to specify conditionals as data rather than code"}
    dictim.conditions)


;; Conditions are useful in over the wire scenarios


(def ^{:private true} comparators
  {"=" =
   "!=" not=
   "contains" some
   "doesnt-contains" (complement some)
   ">" >
   "<" <
   "<=" <=
   ">=" >=})


;; *****************************************
;; *              Conditions               *
;; *****************************************

(defn- get*
  "A generalized version of get/ get-in.
   If k is a keyword/ string, performs a normal get from the map m, otherwise
   if k is a vector of keywords/ strings performs a get-in."
  [m k]
  (cond
    (keyword? k)         (k m)
    (string? k)          (get m k)
    (and (vector? k)
         (every? #(or (string? %) (keyword? %)) k))
    (get-in m k)
    
    :else (throw (Exception. (str "Key must be a keyword, string or vector of either.")))))


(defn- contains-vectors?
  "Returns true if coll contains one or more vectors."
  [coll]
  (some vector? coll))


(defmacro ^{:private true} single-test
  "Returns code that tests whether the condition is true for the item
   specified by sym."
  [sym condition]
  `(let [[comparator# func# v#] ~condition
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


(defn- single-test? [maybe-test]
  (and
   (vector? maybe-test)

   (get comparators (first maybe-test))

   (get accessors (second maybe-test))))


(defmacro test
  "Returns code that tests whether the condition/s is/are true for the item
   specified by sym."
  [sym condition]
  `(if (contains-vectors? ~condition)
     (if (contains? #{:or "or"} (first ~condition))
       (some identity (map #(single-test ~sym %) (rest ~condition)))
       (every? identity (map #(single-test ~sym %) (rest ~condition))))
     (single-test ~sym ~condition)))

;; *****************************************
;; *            validation                 *
;; *****************************************

(defn- valid-single-condition?
  [condition]
  (and
   ;; is a vector
   (vector? condition)

   ;; the first item is a comparator
   ;; (in either keyword or name/string form).
   (or (some #{(first condition)} (keys comparators))
       (some #{(first condition)} (map name (keys comparators))))

   ;; has 3 elements
   (= 3 (count condition))))


(defn- valid-condition?
  [condition]
  (if (and (vector? condition) (contains-vectors? condition))
    (and
     (let [comp (first condition)]
       (some #{comp} [:or :and]))
     (every? valid-single-condition? (rest condition)))
    (valid-single-condition? condition)))
