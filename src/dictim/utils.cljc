(ns ^{:author "judepayne"
      :doc "Namespace for common functions."}
    dictim.utils
  (:refer-clojure :exclude [list?])
  (:require [clojure.string :as str]))


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
    (= :empty-lines (first e))         :empty-lines
    (map? e)                           :attrs
    (kstr? e)                          :quikshape
    (= :comment (first e))             :cmt
    (= :list (first e))                :list
    (not (empty? (filter vector? e)))  :ctr
    (direction? (second e))            :conn
    :else                              :shape))


(defn ctr? [e] (= :ctr (elem-type e)))


(defn attrs? [e] (= :attrs (elem-type e)))


(defn conn? [e] (= :conn (elem-type e)))


(defn shape? [e] (= :shape (elem-type e)))


(defn cmt? [e] (= :cmt (elem-type e)))


(defn list? [e] (= :list (elem-type e)))



(defn prn-repl
  "Prints multiline strings nicely in the repl"
  [d2]
  (run! println (str/split d2 #"\\n")))


(defn line-by-line
  [a b]
  (dorun (map-indexed
          (fn [index item] (let [same? (= (nth b index) item)]
                             (if (not same?)
                               (do (println "-- Mismtach in form " index "-----")
                                   (println "-- first:")
                                   (println item)
                                   (println "-- second:")
                                   (println (nth b index))
                                   (println "--------------------------------")))))
          a))
  nil)
