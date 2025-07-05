(ns ^{:author "Jude payne"
      :doc "Namespace for validating dictim."}
    dictim.validate
  (:require [clojure.string :as str]
            [dictim.d2.attributes :as atd2]
            [dictim.utils :as utils
             :refer [kstr? direction? take-til-last elem-type error list? commented-attr?
                     unquoted-period-or-ampersand-or-bang single-quoted no-asterisk
                     convert-key ctr? shape? quikshape? unquoted-period-or-bang]])
  (:refer-clojure :exclude [list?])
  #?(:cljs (:require-macros [dictim.validate :refer [check]])))


;; validation

;; a dynamic var to hold whether :d2 or :dot is the output format.
(def ^:dynamic ^:private output nil)


;; a dynamic var to whether we are inside a d2 'style'.
(def ^:dynamic ^:private in-style? false)


;; an atom to hold a queue of elements being processed
(def ^:private elem-q (atom []))


(defn- push-elem [elem]
  (swap! elem-q conj elem))


(defn- style? [k]
  (= "style" (convert-key k)))


(defn- vars? [k]
  (= "vars" (convert-key k)))


(defn- classes? [k]
  (= "classes" (convert-key k)))


(defn- err [msg]
  (throw (error msg)))


(defmulti ^:private valid? elem-type)


#?(:clj
   (defmacro ^:private check
     "Creates a multimethod with name `valid?` for the dispatch value
      `dispatch-val`. The body should return true/ false depending on
       whether the element defined by `arg-name` is valid."
     [dispatch-val arg-name body]
     (let [arg (symbol arg-name)]
       `(defmethod ~(symbol "valid?") ~dispatch-val [~arg]
          (if (do (push-elem ~arg) ~body)
            true
            (dictim.validate/err (str ~arg " is invalid.")))))))


(defn- valid-single-connection?
  [[k1 dir k2 & opts]]
  (and (and (kstr? k1) (not (vars? k1)))
       (direction? dir)
       (and (kstr? k2) (not (vars? k2)))
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
        (and (and (kstr? k) (not (vars? k)))
             (direction? d)))
      conns)
     (case (count opts)
       0          true
       1          (or (kstr? (first opts)) (nil? (first opts)) (map? (first opts)))
       2          (and (or (kstr? (first opts)) (nil? (first opts))) (map? (second opts)))
       false))))


;; the defmethods are defined by the 'check' macro in dictim.macros
(check :list li
  (and (or (and (every? #(or (ctr? %) (shape? %) (quikshape? %)) (rest li))
                (every? valid? (rest li)))
                (every? kstr? (rest li)))
             (every? (complement list?) (rest li))))


(defn- key-parts [s]
  (-> s convert-key (str/split unquoted-period-or-bang)
      (->> (remove #(= "" %)))))


(defn- last-key-part [s]
  (-> s key-parts last))


(defn- first-key-part [s]
  (-> s key-parts first))


(defn- d2-key-parts [s]
  (->> s key-parts (filter atd2/key?)))


(defn- just-d2-key [s]
  (let [ps (d2-key-parts s)]
    (when (first ps)
      (str/join "." ps))))


(defn- restruc-attr
  "Normalizes a map entry, stringifying keys and nesting any composite keys."
  [[k v]]
  (let [parts (key-parts k)]
    (letfn [(down [parts]
              (if (next parts)
                {(first parts) (down (next parts))}
                {(first parts) v}))]
      [(first parts) (if (next parts)
                       (down (next parts))
                       v)])))


;; var to hold whether we're in non-d2-keyword part of nest attr map.
;; this can  only occur at the head of the map, e.g. with classes.
(def ^:private ^:dynamic *non-d2-pre?* false)


(defn- valid-d2-attr?
  "Validates a d2 attr. k may be a composite keyword e.g. aShape.style.fill
   and v a value or a (nested) map. When the key is composite, can have a number of
   keys which are not d2 keywords, followed by a number of keys which are. non d2
   keywords are only allowed at the beginning when the first is 'classes' or 'vars'
   (or the internal dynamic var *non-pre-d2?* is bound to true)."
  ([[k v]] (valid-d2-attr? [k v] []))
  ([[k v] ctx]
   (let [[k v] (restruc-attr [k v])]
     (and (kstr? k)
          (cond
            (commented-attr? [k v]) true ;; commented out attrs are not validated
            
            (list? v)             (valid? v)

            (vars? k)             true ;; no further validation necessary

            (classes? k)          (if (map? v)
                                    (binding [*non-d2-pre?* true]
                                      (every? valid-d2-attr? v))
                                    (err "The value of 'classes' must be a map."))
            
            (and (map? v)
                 (atd2/key? k))    (let [elem (peek @elem-q)]
                 (when (atd2/in-context? k ctx elem)
                   (and (atd2/validate-attrs elem k (keys v))
                        (binding [*non-d2-pre?* false]
                          (every? #(valid-d2-attr? % (conj ctx k)) v)))))
            
            (atd2/key? k)      (let [elem (peek @elem-q)]
                                 (when (atd2/in-context? k ctx elem)
                                   (atd2/validate-attr elem k v)))

            
            (and *non-d2-pre?*
                 (map? v))        (every? valid-d2-attr? v)

            
            :else                 (err (str " unknown d2 keyword: " k)))))))


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

;; for cases when the key embeds attr keys e.g. :myShape.style.fill "red"
(defn- valid-inline-d2-attr?
  [elem]
  (let [[k & opts] elem]
    (if-let [d2-k (just-d2-key k)]
      (valid? {d2-k (first opts)})
      (if (map? (first opts))
        (valid? (first opts))
        true))))


(defn- valid-inline-attr? [attr]
  (case output
    :d2 (valid-inline-d2-attr? attr)
    :dot true))


(check :shape elem
       (let [[k & opts] elem]
         (and (and (kstr? k) (not (vars? k)))
              (valid-inline-attr? elem)
              (case (count opts)
                0 true
                1 (or (kstr? (first opts))
                      (nil? (first opts))
                      (map? (first opts)))
                2 (let [[label attrs] opts]
                    (and (or (kstr? label) (nil? label))
                         (and
                          (map? attrs)
                          (every? valid-attr? attrs))))
                false))))


(check :quikshape _ true)


(check :conn conn
       (let [num-dirs (count (filter direction? conn))]
         (if (> num-dirs 1)
           (valid-multiple-connection? conn)
           (valid-single-connection? conn))))


(check :conn-ref cr
       (let [attr (last cr)]
         (or (nil? attr) ;; conn-ref's can be nulled out
             (valid? attr))))


(check :cmt cmt true) ;; no need to validate as utils/elem-type recognition is the validation


(check :ctr elem
       (let [[k & opts] elem]
        (and
         (and (kstr? k) (not (vars? k)))
         (valid-inline-attr? elem)
         (or (and (or (nil? (first opts)) (kstr? (first opts))) ;; label & attrs
                  (valid? (second opts))
                  (or (nil? (rest (rest opts)))
                      (every? valid? (rest (rest opts)))))
             (and (or (nil? (first opts)) (kstr? (first opts))) ;; just the label
                  (or (nil? (rest opts))
                      (every? valid? (rest opts))))
             (and (if (style? (last-key-part k))
                        (binding [in-style? true] (valid? (first opts)))
                        (valid? (first opts))) ;; just the attrs
                  (every? valid? (rest opts)))
             (every? valid? opts) ;; no label or attrs
             (nil? opts)))))


(check :empty-lines elem
       (and (= 2 (count elem))
            (or (= :empty-lines (first elem)) (= "empty-lines" (first elem)))
           (integer? (second elem))
           (> (second elem) 0)))


(check :nil elem
       false)


(check :unknown elem
       false)


;; added for d2 0.7.0 compatibility. See d2/compile.cljc
(check :vars vars-map
  (and (map? vars-map)
       (= 1 (count vars-map))          ; Should have exactly one entry
       (let [[k v] (first vars-map)]
         (and (vars? k)           ; Key should be "vars"
              (map? v)            ; Value should be a map of variables
              ;; Validate the vars content - allow any key-value pairs
              ;; Special case: d2-legend can contain a list
              (every? (fn [[var-key var-val]]
                        (and (kstr? var-key)
                             (or (kstr? var-val)
                                 (map? var-val)
                                 (and (list? var-val)
                                      (= "d2-legend" (convert-key var-key))))))
                      v)))))

(check :classes classes-map
  (and (map? classes-map)
       (= 1 (count classes-map))  ; Should have exactly one entry
       (let [[k v] (first classes-map)]
         (and (classes? k)  ; Key should be "classes"
              (map? v)      ; Value should be a map of class definitions
              ;; Each class should have a map of attributes
              (every? (fn [[class-name class-attrs]]
                        (and (kstr? class-name)
                             (map? class-attrs)
                             ;; Validate class attributes using existing attr validation
                             (binding [*non-d2-pre?* true]
                               (every? valid-d2-attr? class-attrs))))
                      v)))))


(defn all-valid?
  "Validates a collection of dictim elements.
   Throws an error at the first non valid element. Returns nil
   if all elements pass validation.
   output-format is either :d2 or :dot"
  [elems output-format]
  {:pre [(contains? #{:d2 :dot} output-format)]}

  (binding [output output-format]
    (and (seq elems)
         (every? valid? elems))))
