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


(defn elem-type
  "Returns the type of dictim element e."
  [e]
  (cond
    (map? e)                           :attrs
    (and (= 2 (count e))
         (= :comment (first e)))       :cmt
    (not (empty? (filter vector? e)))  :ctr
    (= 1 (count e))                    :shape
    (direction? (second e))            :conn
    :else                              :shape))
