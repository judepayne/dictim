(ns dictim.compile
  ^{:Author "Jude Payne"
    :doc "Namespace for transpiling dictim to d2"}
  (:require [clojure.string :as str]
            [dictim.format :as f]
            [dictim.utils :refer [kstr? direction? take-til-last elem-type]]
            [dictim.validate :refer [valid?]])
  (:refer-clojure :exclude [list]))


(def colon ": ")

(def spc " ")


;; sep the separator between elements
(def ^:dynamic sep)

;; ml = multiline, i.e. not compressed into a line (as in a list).
(defn ml? [] (= \newline sep))


(defn- de-key
  [s]
  (if (keyword? s)
    (name s)
    s))


(defn- attrs
  "layout the map of attrs. m may be nested."
  ([m] (attrs m true))
  ([m brackets?]
   (apply str
          (when brackets? "{")
          (when (ml?) \newline)
          (apply str
                 (->>
                  (for [[k v] m]
                    (cond
                      (map? v)   (str (name k) colon (attrs v))
                      :else      (str (name k) colon (de-key v))))
                  (interpose sep)))
          (if (ml?) \newline sep)
          (when brackets? "}"))))


(defn item->str [i]
  (cond
    (kstr? i) (name i)
    (map? i)  (attrs i)))


(defn- optionals
  "opts is a vector of label and attrs - both optional. converts to string."
  [opts]
  (apply str
         (interpose spc (map item->str opts))))


(defn- single-conn
  "layout conn(ection) vector."
  [[k1 dir k2 & opts]]
  (str (name k1) spc dir spc (name k2)
       (when opts (str colon (optionals opts))) sep))


(defn- multi-conn
  "layout multiple connections vector."
  [c]
  (let [[kds [lk & opts]] (take-til-last direction? c)]
    (str (apply str (interpose spc (map item->str (conj (into [] kds) lk))))
         (when opts (str colon (apply str (interpose spc (map item->str opts)))))
         sep)))


(defmulti ^:private layout elem-type)


(defmethod layout :attrs [el] (attrs el false))


(defmethod layout :shape [[k & opts]]
  (str (name k) (when opts colon) (optionals opts) sep))


(defmethod layout :quikshape [el]
  (str (name el) sep))


(defmethod layout :conn [el]
  ;establish whether we have a single connection or multiple
  (let [num-dirs (count (filter direction? el))]
    (if (> num-dirs 1)
      (multi-conn el)
      (single-conn el))))


(defmethod layout :cmt [el]
  (str "# " (second el) sep))


(defmethod layout :list [li]
  (str
   (binding [sep \;]
     (apply str (map layout (butlast (rest li)))))
   (binding [sep ""]
     (layout (last li)))
   sep))


(defmethod layout :ctr [[k & opts]]
  (str (name k) colon
       (when (kstr? (first opts)) (name (first opts)))
       " {"
       sep
       (apply str
              (map
               (fn [i]
                 (cond
                   (map? i)    (attrs i false)
                   (vector? i) (layout i)))
               opts))
       (str "}" sep)))



(defn d2
  "Converts dictim elements to a well formatted d2 string.
   Validates each element, throws an error if invalid."
  [& elems]

  (run! valid? elems)

  (binding [sep \newline]
    (-> (apply str (mapcat layout elems))
        (f/fmt :tab 2))))


(comment (time (dotimes [n 1000000] (binding [sep \newline] (apply str (mapcat element ex))))))
;; 54185 ms for ex

