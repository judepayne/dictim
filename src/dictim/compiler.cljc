(ns dictim.compiler
  (:require [clojure.string :as str]
            [dictim.format :as f]))


(defn error
  "Creates an exception object with error-string."
  [error-string]
  #?(:clj (Exception. ^String error-string)
     :cljs (js/Error. error-string)))


;;indentation


(def ^:private indentation-counter (atom 0))


(def ^:private tab (atom 0))


(defn- ind! [] (swap! indentation-counter #(+ @tab %)) nil)


(defn- outd! [] (swap! indentation-counter #(- % @tab)) nil)


(defn- tabs [] (apply str (repeat @indentation-counter \space)))


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
   (every? #(or (kstr? %) (list? %) (number? %) (and (map? %) (valid-attrs? %))) (vals m))))


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
  (and  (kstr? k1)
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
  (let [conns (partition 2 c)]
    (every?
     (fn [[k d]]
       (and (kstr? k) (direction? d)))
     conns)))


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


(defn- elem-type
  [e]
  (cond
    (not (empty?(filter vector? e)))   :ctr
    (= 1 (count e))                    :shape
    (direction? (second e))            :conn
    :else                              :shape))


(defn valid-element?
  "Validates the dictim element. Throws an error if not valid."
  [e]
  (cond
    (map? e)       (valid-attrs? e)
    (vector? e)    (case (elem-type e)
                     :shape (valid-shape? e)
                     :conn  (valid-connection? e)
                     :ctr   (valid-container? e)
                     false)
    :else (throw (error (str "Element " e " must be either a map or a vector.")))))


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
          (when brackets?
            (str "}" @sep)))))


(defn- optionals
  "opts is a vector of label and attrs - both optional. converts to string."
  [opts]
  (apply str
         (interpose space
                    (map
                     (fn [i]
                       (cond
                         (kstr? i) (name i)
                         (map? i)  (attrs i)))
                     opts))))


(defn- shape
  "layout shape vector."
  [[k & opts]]
  (str (name k) colon (optionals opts) @sep))


(defn- single-conn
  "layout conn(ection) vector."
  [[k1 dir k2 & opts]]
  (str (name k1) space dir space (name k2) colon (optionals opts) @sep))


(defn- multi-conn
  "layout multiple connections vector."
  [c]
  (str (apply str (interpose space (map (fn [i] (name i)) c))) @sep))


(defn- conn
  "layout connection vector."
  [c]
  ;establish whether we have a single connection of multiple
  (let [num-dirs (count (filter direction? c))]
    (if (> num-dirs 1)
      (multi-conn c)
      (single-conn c))))


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
  (if (map? e)  ;; <-- diagram level options. come as the first element
    (attrs e false)
    (case (elem-type e)
      :shape (shape e)
      :conn  (conn e)
      :ctr   (ctr e))))


; --------


(defn- options? [m]
  (and (map? m)
       (or (contains? m :separator)
           (contains? m :format?)
           (contains? m :tab))))


(defn- handle-opts-pre
  [{:keys [separator] :or {separator "\n"} :as opts}]
  ;; actions setting internal compiler options
  (reset! sep separator))


(defn- post-process
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
    (handle-opts-pre opts))
  
  (let [elems (if (options? opts) els elems)]
    (mapv valid-element? elems)
    (-> (apply str (mapcat element elems))
        (post-process (when (options? opts) opts)))))
