(ns dictim.compile
  (:require [clojure.string :as str]
            [dictim.format :as f]
            [dictim.attributes :as at]))


(defn error
  "Creates an exception object with error-string."
  [error-string]
  #?(:clj (Exception. ^String error-string)
     :cljs (js/Error. error-string)))


;; validation

(defn- direction?
  [s]
  (contains? #{"--" "<->" "->" "<-"} s))


(defn- kstr?
  "Value is either a keyword or a string?"
  [v]
  (or (keyword? v) (string? v)))


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


(defn- take-til-last
  "take until the last match. not every item needs match"
  [pred coll]
  (-> (reduce
         (fn [acc cur]
           (cond
             (nil? cur)    (:banked acc)
             
             (pred cur)    (-> acc
                               (update :banked concat (conj (:buffer acc) cur))
                               (assoc-in [:buffer] []))
             
             :else         (update acc :buffer conj cur)))
         {:banked [] :buffer []}
         coll)
      vals))


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


(defn- elem-type
  [e]
  (cond
    (map? e)                           :attrs
    (and (= 2 (count e))
         (= :comment (first e)))       :cmt
    (not (empty?(filter vector? e)))   :ctr
    (= 1 (count e))                    :shape
    (direction? (second e))            :conn
    :else                              :shape))


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


;; Compilation

(def colon ": ")

(def space " ")

;; atom to hold the separator. defaulted later in '\n'
(def sep (atom \newline))


(defn- de-key
  [s]
  (if (keyword? s)
    (name s)
    s))


(defn- flat-attrs
  [m]
  (str "{" (apply str (interpose "; " (map (fn [[k v]] (str (name k) ": " v)) m))) "}"))


(defn- handle-list
  [k v]
  (cond
    (= "order" (name k))  (apply str (interpose "; " (map name v)))
    :else                 (str (name k)
                               space
                               (apply str (interpose
                                           space
                                           (map
                                            (fn [item]
                                              (cond
                                                (map? item) (flat-attrs item)
                                                :else item))
                                            v))))))


(defn- attrs
  "layout attrs (to be supplied as a map)."
  ([m] (attrs m true))
  ([m brackets?]
   (apply str
          (when brackets? "{")
          (apply str
                 (for [[k v] m]
                   (cond
                     (map? v)   (str (name k) colon (attrs v))
                     (list? v)  (str (handle-list k v) @sep)
                     :else (str (name k) colon (de-key v) @sep))))
          (when brackets? "}"))))


(defn item->str [i]
  (cond
    (kstr? i) (name i)
    (map? i)  (attrs i)))


(defn- optionals
  "opts is a vector of label and attrs - both optional. converts to string."
  [opts]
  (apply str
         (interpose space (map item->str opts))))


(defn- shape
  "layout shape vector."
  [[k & opts]]
  (str (name k) colon (optionals opts) @sep))


(defn- single-conn
  "layout conn(ection) vector."
  [[k1 dir k2 & opts]]
  (str (name k1) space dir space (name k2) (when opts (str colon (optionals opts))) @sep))


(defn- multi-conn
  "layout multiple connections vector."
  [c]
  (let [[kds [lk & opts]] (take-til-last direction? c)]
    (str (apply str (interpose space (map item->str (conj (into [] kds) lk))))
         (when opts
           (str ": " (apply str (interpose space (map item->str opts)))))
         @sep)))


(defn- conn
  "layout connection vector."
  [c]
  ;establish whether we have a single connection or multiple
  (let [num-dirs (count (filter direction? c))]
    (if (> num-dirs 1)
      (multi-conn c)
      (single-conn c))))


(defn- cmt
  "layout comment vector"
  [c]
  (str "# " (second c) @sep))


(declare element)


(defn- ctr
  "layout ctr (container) vector, which may be nested."
  [[k & opts]]
  (str (name k) colon
       (when (kstr? (first opts)) (name (first opts)))
       " {"
       @sep
       (apply str
              (map
               (fn [i]
                 (cond
                   (map? i)    (attrs i false)
                   (vector? i) (element i)))
               opts))
       (str "}" @sep)))


(defn- element
  "layout element (ctr, shape, conn), which may be nested if ctr (container)."
  [e]
  (case (elem-type e)
    :cmt     (cmt e)
    :attrs   (attrs e false)
    :shape   (shape e)
    :conn    (conn e)
    :ctr     (ctr e)))


; --------


(defn- options?
  "Is m a map of pre & post processing options, rather than an element?"
  [m]
  (and (map? m)
       (or (contains? m :separator)
           (contains? m :format?)
           (contains? m :tab))))


(defn- pre-process
  "Set the separator for d2 terms."
  [{:keys [separator] :or {separator "\n"} :as opts}]
  (reset! sep separator))


(defn- post-process
  "Controls formatting post processing step."
  [out {:keys [format? tab] :or {format? true tab 2} :as opts}]
  (if format?
    (f/fmt out :tab tab)
    out))


(defn d2
  "Converts dictim elements to d2. Each element can be either:
    - a `shape`  which has the form [<key> <label>(optional) <attribute-map>(optional)
    - a `connection` [<src-key> <direction*> <dest-key> <label>(opt) <attribute-map(opt)
    - a `container` [<key> <label>(opt) <attribute-map>(optimal) & further nested elements]
    - an `attribute-map` {<key1> <val1> .. <keyN> <valN>}}

   *direction can be either \"--\" (undirected) or \"->\" (directed).
   The first argument supplied can optionally  be a map of options:
      :separator default: '\n', the separator to use between d2 terms. Both ';' or '\n' are valid.
      :format?   default: true, whether the output string should be formatted.
      :tab       default: 2, the indentation step to use in formatting.

  Attempts to validate the supplied elements and throws an error for the incorrect element.

  Example usage:
      (d2 [:studentA \"Salacious B. Crumb\" {:style.fill \"red\"}])
  => 'studentA: Salacious B. Crumb {\n  style.fill: red\n}\n'
  "
  [& [opts & els :as elems]]
  
  ;; if an elem is a map (i.e. the diagram level options), it  must be first
  {:pre [(or (and (not (empty? (filter map? elems))) (map? (first elems)))
             (every? vector? elems))]}

  (if (options? opts)
    (pre-process opts))
  
  (let [elems (if (options? opts) els elems)]
    (mapv valid-element? elems)
    (-> (apply str (mapcat element elems))
        (post-process (when (options? opts) opts)))))
