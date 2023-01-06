(ns
    ^{:author "Jude payne"
      :doc "Namespace for validating dictim."}
    dictim.validate
  (:require [dictim.attributes :as at]
            [dictim.utils :refer [kstr? direction? take-til-last elem-type error]]))


;; validation


(defn- valid-attrs?
  [m]
  (and
   (map? m)
   (every? kstr? (keys m))
   (every? at/d2-keyword? (keys m))
   (every? #(or (kstr? %) (list? %) (number? %)
                (and (map? %) (valid-attrs? %)))
           (vals m))))


(defn- valid-shape?
  [[k & opts]]
  (and (kstr? k)
       (case (count opts)
         0 true
         1 (or (kstr? (first opts))
               (valid-attrs? (first opts)))
         2 (let [[label attrs] opts]
             (and (kstr? label)
                  (valid-attrs? attrs)))
         false)))


(defn- valid-single-connection?
  [[k1 dir k2 & opts]]
  (and (kstr? k1)
       (direction? dir)
       (kstr? k2)
       (case (count opts)
         0 true
         1 (or (kstr? (first opts))
               (valid-attrs? (first opts)))
         2 (let [[label attrs] opts]
             (and (kstr? label)
                  (valid-attrs? attrs)))
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


(defn- valid-connection?
  [c]
  ;establish whether we have a single connection or multiple
  (let [num-dirs (count (filter direction? c))]
    (if (> num-dirs 1)
      (valid-multiple-connection? c)
      (valid-single-connection? c))))


(declare valid-element?)


(defn- valid-container?
  [[k & opts]]
  (and
   (kstr? k)
   (or (and (kstr? (first opts))           ;; label & attrs
            (valid-attrs? (second opts))
            (or (nil? (rest (rest opts)))
                (every? valid-element? (rest (rest opts)))))
       (and (kstr? (first opts))          ;; just the label
            (or (nil? (rest opts))
                (every? valid-element? (rest opts))))
       (and (valid-attrs? (first opts))   ;; just the attrs
            (every? valid-element? (rest opts)))
       (every? valid-element? opts)       ;; no label or attrs
       (nil? opts))))                     ;; empty container (is permitted)


(defn- valid-comment?
  [c]
  (and (= 2 (count c))
       (= :comment (first c))
       (string? (second c))))


(defn valid-element?
  "Validates the dictim element. Throws an error if not valid."
  [e]
  (let [valid?
        (case (elem-type e)
          :attrs           (valid-attrs? e)
          :shape           (valid-shape? e)
          :conn            (valid-connection? e)
          :ctr             (valid-container? e)
          :cmt             (valid-comment? e))]
    (if valid?
      true
      (throw (error (str "Element " e " is not valid."))))))
