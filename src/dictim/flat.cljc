(ns
    ^{:author "judepayne"
      :doc "Namespace for flattening and (re)building dictim."}
    dictim.flat
  (:require [dictim.utils :refer [kstr? direction? take-til-last elem-type error
                                  ctr? list? cmt?]]
            [clojure.walk :as walk])
  (:refer-clojure :exclude [list? flatten]))

;; This namespace provide functions for taking (nested) dictim elements and flattening
;; them into a sequence of 'flat-dictim' maps of standard form:
;; {:key <...>
;;  :type  <:ctr/"ctr"/:list/"list"/:shape/"shape"/:cmt/"cmt"/:attrs/"attrs"/:conn/"conn"/:conn-ref/"conn-ref"/:empty-lines/"empty-lines">
;;  :meta <..>
;;  :parent <..> }
;; as well as building a seuqnece of flat-dictim back up into dictim


(def ^:private keys-to-keywordize
  #{"key" "type" "meta" "parent"})


(defn- keywordize
  "keywordize the keys required by dictim.flat and the value of the :type key"
  [m]
  (let [f (fn [[k v]]
            (let [k (if (contains? keys-to-keywordize k) (keyword k) k)
                  v (if (= :type k) (keyword v) v)]
              [k v]))]
    (walk/postwalk (fn [x] (if (map? x) (into {} (map f x)) x)) m)))


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
  (cond
    (and (nil? f) (nil? s))  nil
    s {:label f :attrs s}
    :else  (if (map? f) {:attrs f} {:label f})))


(defn- conn-meta
  [c]
  (if (single-conn? c)
    (let [[f s] (drop 3 c)]
      (fs f s))
    (let [[_ [lk f s]] (take-til-last direction? c)]
      (fs f s))))


(defn- conn-ref-meta
  [c]
  (let [r (drop 3 c)]
    (merge {:index (first r)}
           (fs (first (rest r)) (second (rest r))))))


(defn- quick-shape-meta ;; allow for 'quick shape'
  [sh]
  nil)


(defn- shape-meta
  [sh]
  (let [[_ f s] sh]
    (fs f s)))


(defn- ctr-meta
  [c]
  (let [[f s] (take-while (complement vector?) (rest c))]
    (fs f s)))


(defn- elem-key
  ([e] (elem-key e (elem-type e)))
  ([e elem-type]
   (case elem-type
     :cmt e
     :attrs e
     :shape (if (vector? e) (first e) e)  ;; allow for 'quick shape'
     :ctr (first e)
     :conn (conn-key e)
     :conn-ref (conn-key e)
     :list (map elem-key (rest e))
     :empty-lines (second e))))


(defn- elem-meta
  ([e] (elem-meta e (elem-type e)))
  ([e elem-type]
   (case elem-type
     :shape (if (vector? e) (shape-meta e) (quick-shape-meta e))  ;; allow for 'quick shape'
     :ctr (ctr-meta e)
     :cmt nil
     :attrs nil
     :conn (conn-meta e)
     :conn-ref (conn-ref-meta e)
     :list nil
     :empty-lines nil)))


;; flattening

(defn- filter-nil [m]
  (reduce-kv
   (fn [m k v]
     (if (nil? v) m (assoc m k v)))
   {}
   m))


(defn- describe-elem
  [e parent]
  (let [t (elem-type e)]
    (-> {:type t
         :key (elem-key e t)
         :meta (elem-meta e t)
         :parent (:key parent)}
        filter-nil)))


(defn- children
  [e]
  (case (elem-type e)
    :ctr     (filter #(or (vector? %) (cmt? %)) e)
    :list    (rest e)))        ;; allow for 'quick shape'


(defn- tree-seq'
  "Like tree-seq but with f a transform function which is applied to each
   node and its (transformed) parent."
  [f branch? children root]
   (let [walk (fn walk [parent node]
                (lazy-seq
                 (let [new-node (f node parent)]
                   (cons new-node
                         (when (branch? node)
                           (mapcat (partial walk new-node) (children node)))))))]
     (walk nil root)))


(defn flatten
  "Flattens dictim elements into flat-dictim format."
  [& elems]
  (mapcat (fn [root]
            (tree-seq'
             describe-elem
             #(or (ctr? %) (list? %))
             children
             root))
         elems))

;; building


(defn- rnil [coll]
  (into [] (remove nil? coll)))


(defn- build-elem
  [m]
  (case (:type m)
    :shape    (rnil [(:key m) (-> m :meta :label) (-> m :meta :attrs)])
    :cmt      (:key m)
    :attrs    (:key m)
    :conn     (rnil (conj (:key m) (-> m :meta :label) (-> m :meta :attrs)))
    :conn-ref (rnil (conj (:key m) (-> m :meta :index) (-> m :meta :label) (-> m :meta :attrs)))
    :ctr      (rnil [(:key m) (-> m :meta :label) (-> m :meta :attrs)])
    :list     [:list]
    :empty-lines [:empty-lines (:key m)]))


(defn build
  "Builds a sequence of flat-dictim elements into dictim."
  [flat-elems]
  (let [flat-elems (map keywordize flat-elems)
        children (group-by :parent flat-elems)
        nodes (fn nodes [parent-id]
                (map
                 (fn [elem]
                   (if (or (= :ctr (:type elem)) (= :list (:type elem)))
                     (reduce conj (build-elem elem) (nodes (:key elem)))
                     (build-elem elem)))
                 (get children parent-id)))]
    (nodes nil)))
