(ns dictim.compiler
  (:require [clojure.string :as str]))


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

(defn- valid-direction?
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


(defn- valid-connection?
  [[k1 dir k2 & opts]]
  (and  (kstr? k1)
       (valid-direction? dir)
       (kstr? k2)
       (case (count opts)
         0 true
         1 (or (kstr? (first opts))
               (valid-attrs? (first opts)))
         2 (let [[label attrs] opts]
             (and (kstr? label)
                  (valid-attrs? attrs)))
         false)))


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
    (valid-direction? (second e))      :conn
    :else                              :shape))


(defn valid-element?
  "Validates the dictim element. Throws an error if not valid."
  [e]
  (if
      (and
       (vector? e)
       (case (elem-type e)
         :shape (valid-shape? e)
         :conn  (valid-connection? e)
         :ctr   (valid-container? e)
         false))
      true
      (throw (error (str "Element " e " failed to validate.")))))


;; Compilation

(def colon ": ")

(def space " ")


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
          (when brackets? (str "{" \newline (ind!)))
          (apply str
                 (for [[k v] m]
                   (cond
                     (map? v)   (str (tabs) (name k) colon (attrs v))
                     (list? v)  (str (tabs) (handle-list k v) \newline)
                     :else (str (tabs) (name k) colon (de-key v) \newline))))
          (when brackets?
            (str (outd!) (tabs) "}" \newline)))))


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
  (str (tabs) (name k) colon (optionals opts) \newline))


(defn- conn
  "layout conn(ection) vector."
  [[k1 dir k2 & opts]]
  (str (tabs) (name k1) space dir space (name k2) colon (optionals opts) \newline))


(declare element)


(defn- ctr
  "layout ctr (container) vector, which may be nested."
  [[k & opts]]
  (str (tabs) (name k) colon
       (when (kstr? (first opts)) (name (first opts)))
       " {"
       \newline
       (ind!)
       (apply str
              (map
               (fn [i]
                 (cond
                   (map? i)    (attrs i false)
                   (vector? i) (element i)))
               opts))
       (str (outd!) (tabs) "}" \newline)))


(defn- element
  "layout element (ctr, shape, conn), which may be nested if ctr (container)."
  [e]
  (if (map? e)  ;; <-- diagram level options. come as the first element
    (attrs e false)
    (case (elem-type e)
      :shape (shape e)
      :conn  (conn e)
      :ctr   (ctr e))))


(defn- handle-opts
  [{:keys [tab-spacing] :or {tab-spacing 2} :as opts}]
  ;; actions setting internal compiler options
  (reset! tab tab-spacing)

  (dissoc opts :tab-spacing))


(defn d2
  "Converts dictim elements to d2. Each element can be either:
    - a `shape`  which has the form [<key> <label>(optional) <attribute-map>(optional)
    - a `connection` [<src-key> <direction*> <dest-key> <label>(opt) <attribute-map(opt)
    - a `container` [<key> <label>(opt) <attribute-map>(optimal) & further nested elements]

   *direction can be either \"--\" (undirected) or \"->\" (directed).
   The first argument supplied can be a map of diagram level attributes e.g. {:direction \"right\"}
   Attempts to validate the supplied elements and throws an error for the incorrect element."
  [& [opts & els :as elems]]
  
  ;; if an elem is a map (i.e. the diagram level options), it  must be first
  {:pre [(or (and (not (empty? (filter map? elems))) (map? (first elems)))
             (every? vector? elems))]}

  ;; counter used to keep track of indentation for well formatted layout.
  (reset! indentation-counter 0)
  (reset! tab 2)
  
  (if (map? opts)
    (let [opts (handle-opts opts)]
      (mapv valid-element? els)
      (apply str (mapcat element (cons opts els))))

    (do
      (mapv valid-element? elems)
      (apply str (mapcat element elems)))))
