(ns ^{:author "Jude payne"
      :doc "Namespace for validating dictim."}
    dictim.validate
  (:require [clojure.string :as str]
            [dictim.attributes :as at]
            [dictim.utils :as utils :refer [kstr? direction? take-til-last elem-type error list?
                                  conn-ref? unquoted-period single-quoted no-asterisk convert-key]])
  (:refer-clojure :exclude [list?])
  #?(:cljs (:require-macros [dictim.validate :refer [check]])))


;; validation

;; a dynamic var to hold whether :d2 or :dot is the output format.
(def ^:dynamic output nil)


;; a dynamic var to hold whether we need to check attr keys as d2 keywords.
(def ^:dynamic vars?)


(defn is-vars? [k]
  (= "vars" (convert-key k)))


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
  (and (and (kstr? k1) (not (is-vars? k1)))
       (direction? dir)
       (and (kstr? k2) (not (is-vars? k2)))
       (case (count opts)
         0 true
         1 (or (kstr? (first opts))
               (nil? (first opts))
               (valid? (first opts)))
         2 (let [[label attrs] opts]
             (and (or (kstr? label) (nil? label))
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
        (and (and (kstr? k) (not (is-vars? k)))
             (direction? d)))
      conns)
     (case (count opts)
       0          true
       1          (or (kstr? (first opts)) (nil? (first opts)) (map? (first opts)))
       2          (and (or (kstr? (first opts)) (nil? (first opts))) (map? (second opts)))
       false))))


;; the defmethods are defined by the 'check' macro in dictim.macros
(check :list li
       (and (or (every? valid? (rest li))
                (every? kstr? (rest li)))
             (every? (complement list?) (rest li))))


(defn- last-key-part [s]
  (-> s convert-key (str/split unquoted-period) last))


(defn- first-key-part [s]
  (-> s convert-key (str/split unquoted-period) first))


(defn- valid-d2-attr-key? [k]
  (or (conn-ref? k)
      (number? k)
      (keyword? k)
      (string? k)))


(defn- valid-d2-attr? [[k v]]
  ;; if we're in :vars, don't peform d2-keyword checks. optional; checks
  (binding [vars? (or vars? (= (convert-key k) "vars"))]
    (and
     (valid-d2-attr-key? k)
     (cond
       (list? v)          (valid? v)
       
       (map? v)           (valid? v)

       (conn-ref? k)      true ;; conn-ref is already validated by the detection fn.

       (and (not (map? v))
            (not vars?))
       (let [k' (last-key-part k)]
         (and (at/d2-keyword? k')
              (or
               (let [val-fn (at/validate-fn k')]
                 (val-fn v))
               (nil? v) ;; attributes can be 'nulled'. See the d2 tour overrides page.
               )))

       (and (not (map? v))
            vars?)
       (let [k' (last-key-part k)]
         (if (at/d2-keyword? k')
           (let [val-fn (at/validate-fn k')]
             (val-fn v))
           true))))))


(defn- valid-dot-attr? [[k v]]
  (and
   (kstr? k)
   (if (map? v)
     (valid? v)
     true)))


(defn- valid-attr? [attr]
  (case output
    :d2 (valid-d2-attr? attr)
    :dot (valid-dot-attr? attr)))


(check :attrs m
       (and
        (map? m)
        (every? valid-attr? m)))


(defn globs-quoted? [k]
  (let [k (first-key-part k)]
    (or (re-matches single-quoted k)
        (re-matches no-asterisk k))))


;; globs are allowed in key names but only when the whole name/ first part is quoted.


(check :shape elem
       (let [[k & opts] elem]
         (and (and (kstr? k) (globs-quoted? k) (not (is-vars? k))) 
              (case (count opts)
                0 true
                1 (or (kstr? (first opts))
                      (nil? (first opts))
                      (valid? (first opts)))
                2 (let [[label attrs] opts]
                    (and (or (kstr? label) (nil? label))
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
         (and (kstr? k) (globs-quoted? k) (not (is-vars? k)))
         (or (and (or (nil? (first opts)) (kstr? (first opts))) ;; label & attrs
                  (valid? (second opts))
                  (or (nil? (rest (rest opts)))
                      (every? valid? (rest (rest opts)))))
             (and (or (nil? (first opts)) (kstr? (first opts))) ;; just the label
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
