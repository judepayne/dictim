(ns
    ^{:author "Jude payne"
      :doc "Namespace for validating dictim."}
    dictim.validate
  (:require [clojure.string :as str]
            [dictim.attributes :as at]
            [dictim.utils :refer [kstr? direction? take-til-last elem-type error list?]])
  (:refer-clojure :exclude [list?])
  #?(:cljs (:require-macros [dictim.validate :refer [check]])))


;; validation

;; a dynamic var to hold whether :d2 or :dot is the output format.
(def ^:dynamic output nil)


(defn err [msg]
  (throw (error (str msg " is invalid."))))


(defmulti valid? elem-type)


#?(:clj
   (defmacro check
     "Creates a multimethod with name `valid?` for the dispatch value
      `dispatch-val`. The body should return true/ false depending on
       whether the element defined by `arg-name` is valid."
     [dispatch-val arg-name body]
     (let [arg (symbol arg-name)]
       `(defmethod ~(symbol "valid?") ~dispatch-val [~arg]
          (if ~body
            true
            (dictim.validate/err ~arg))))))


(defn- valid-single-connection?
  [[k1 dir k2 & opts]]
  (and (kstr? k1)
       (direction? dir)
       (kstr? k2)
       (case (count opts)
         0 true
         1 (or (kstr? (first opts))
               (valid? (first opts)))
         2 (let [[label attrs] opts]
             (and (kstr? label)
                  (valid? attrs)))
         false)))


(defn- valid-multiple-connection?
  [c]
  (let [[ds [lk & opts]] (take-til-last direction? c)
        conns (partition 2 ds)]
    (and
     (kstr? lk)
     (every?
      (fn [[k d]]
        (and (kstr? k) (direction? d)))
      conns)
     (case (count opts)
       0          true
       1          (or (kstr? (first opts)) (map? (first opts)))
       2          (and (kstr? (first opts)) (map? (second opts)))
       false))))


;; the defmethods are defined by the 'check' macro in dictim.macros
(check :list li
        (and (every? valid? (rest li))
             (every? (complement list?) (rest li))))


(defn- last-d2-keyword? [k]
  (let [k' (if (keyword? k) (name k) k)]
    (-> k'
        (str/split #"\.")
        last
        at/d2-keyword?)))


(check :attrs m
       (and
        (map? m)
        (every? kstr? (keys m))
        (if (= :d2 output)
          (every? last-d2-keyword? (keys m))
          true)      ;; Graphviz keywords are not checked at this point.
        (every? #(or (and (map? %) (valid? %))
                     (kstr? %) (number? %) (boolean? %)) (vals m))))


(check :shape elem
       (let [[k & opts] elem]
         (and (kstr? k)
              (case (count opts)
                0 true
                1 (or (kstr? (first opts))
                      (valid? (first opts)))
                2 (let [[label attrs] opts]
                    (and (kstr? label)
                         (valid? attrs)))
                false))))


(check :quikshape _ true)


(check :conn conn
       (let [num-dirs (count (filter direction? conn))]
         (if (> num-dirs 1)
           (valid-multiple-connection? conn)
           (valid-single-connection? conn))))


(check :cmt cmt
       (and (= 2 (count cmt))
           (= :comment (first cmt))
           (string? (second cmt))))


(check :ctr elem
       (let [[k & opts] elem]
        (and
         (kstr? k)
         (or (and (kstr? (first opts)) ;; label & attrs
                  (valid? (second opts))
                  (or (nil? (rest (rest opts)))
                      (every? valid? (rest (rest opts)))))
             (and (kstr? (first opts)) ;; just the label
                  (or (nil? (rest opts))
                      (every? valid? (rest opts))))
             (and (valid? (first opts)) ;; just the attrs
                  (every? valid? (rest opts)))
             (every? valid? opts) ;; no label or attrs
             (nil? opts)))))


(check :empty-lines elem
       (and (= 2 (count elem))
           (= :empty-lines (first elem))
           (integer? (second elem))
           (> (second elem) 0)))


(defn all-valid?
  "Validates a collection of dictim elements.
   Throws an error at the first non valid element. Returns nil
   if all elements pass validation.
   output-format is either :d2 or :dot"
  [elems output-format]
  {:pre [(contains? #{:d2 :dot} output-format)]}

  (binding [output output-format]
    (every? valid? elems)))
