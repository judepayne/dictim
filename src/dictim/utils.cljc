(ns ^{:author "judepayne"
      :doc "Namespace for common functions."}
    dictim.utils)


(defn error
  "Creates an exception object with error-string."
  [error-string]
  #?(:clj (Exception. ^String error-string)
     :cljs (js/Error. error-string)))


(defn kstr?
  "Value is either a keyword or a string?"
  [v]
  (or (keyword? v) (string? v)))


(defn direction?
  [s]
  (contains? #{"--" "<->" "->" "<-"} s))


(defn take-til-last
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


;; element types

(defn elem-type
  "Returns the type of dictim element e."
  [e]
  (cond
    (map? e)                           :attrs
    (and (= 2 (count e))
         (= :comment (first e)))       :cmt
    (= :list (first e))                :lst
    (not (empty? (filter vector? e)))  :ctr
    (= 1 (count e))                    :shape
    (direction? (second e))            :conn
    :else                              :shape))


(defn ctr? [e] (= :ctr (elem-type e)))


(defn attrs? [e] (= :attrs (elem-type e)))


(defn conn? [e] (= :conn (elem-type e)))


(defn shape? [e] (= :shape (elem-type e)))


(defn cmt? [e] (= :cmt (elem-type e)))


(defn lst? [e] (= :lst (elem-type e)))


;; destructuring elements

(defn- single-conn?
  [c]
  (= (count (filter direction? c)) 1))


(defn- conn-key
  [c]
  (if (single-conn? c)
    (into [] (take 3 c))
    (let [[kds [lk & _]] (take-til-last direction? c)]
      (conj (into [] kds) lk))))


(defn- fs
  [f s]
  (if s {:label f :attrs s}
      (if (map? f) {:attrs f} {:label f})))


(defn- conn-meta
  [c]
  (if (single-conn? c)
    (let [[f s] (drop 3 c)]
      (fs f s))
    (let [[_ [lk f s]] (take-til-last direction? c)]
      (fs f s))))


(defn- shape-meta
  [sh]
  (let [[_ f s] sh]
    (fs f s)))


(defn- ctr-meta
  [c]
  (let [[f s] (take-while (complement vector?) (rest c))]
    (fs f s)))


(defn elem-key
  ([e] (elem-key e (elem-type e)))
  ([e elem-type]
   (case elem-type
     :cmt (second e)
     :attrs e
     :shape (first e)
     :ctr (first e)
     :conn (conn-key e))))


(defn elem-meta
  ([e] (elem-meta e (elem-type e)))
  ([e elem-type]
   (case elem-type
     :shape (shape-meta e)
     :ctr (ctr-meta e)
     :cmt nil
     :attrs nil
     :conn (conn-meta e))))
