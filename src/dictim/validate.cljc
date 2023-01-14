(ns
    ^{:author "Jude payne"
      :doc "Namespace for validating dictim."}
    dictim.validate
  (:require [dictim.attributes :as at]
            [dictim.utils :refer [kstr? direction? take-til-last elem-type error list?]])
  (:refer-clojure :exclude [list?])
  #?(:cljs (:require-macros [dictim.validate :refer [check]])))


;; validation


(defn err [msg]
  (throw (error (str msg " is invalid."))))


(defmulti valid? elem-type)


#?(:clj
   (defmacro check
     "Creates a multimethod of name `valid?` with dispatch-val.
   condition should check the symbol `elem` for validity."
     [dispatch-val arg-name condition]
     (let [arg (symbol arg-name)]
       `(defmethod ~(symbol "valid?") ~dispatch-val [~arg]
          (if ~condition
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

(check :attrs m
       (and
         (map? m)
         (every? kstr? (keys m))
         (every? at/d2-keyword? (keys m))
         (every? #(or (kstr? %) (list? %) (number? %)
                      (and (map? %) (valid? %))) (vals m))))


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
