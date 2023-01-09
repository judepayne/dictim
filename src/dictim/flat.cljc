(ns
    ^{:author "judepayne"
      :doc "Namespace for disassembling and assembling dictim."}
    dictim.flat
  (:require [dictim.utils :refer [kstr? direction? take-til-last elem-type error
                                  elem-key elem-meta]]))

;; This namespace provide functions for taking (nested) dictim elements and then later
;; (re)assembling as dictim.
;; d2 & dictim merge data (e.g. the keys of shapes, the connections between them) with
;; visualization information, e.g. the fill color for a shape.
;; If dictim needs to be stored (perhaps separating the data from visualization information)
;; for reassembly later, it needs to be converted into a standard flattened form. This is
;; what the 'disassemble' and 'assemble' functions are for.
;; ***************************************************
;; The standard flattened form is called 'flat dictim'
;; ***************************************************


;; disassembling

(defn- describe-elem
  [e]
  (case (elem-type e)
    :cmt     {:type :cmt :key (elem-key e :cmt) :meta (elem-meta e :cmt)}
    :attrs   {:type :attrs :key (elem-key e :attrs) :meta (elem-meta e :attrs)}
    :shape   {:type :shape :key (elem-key e :shape)  :meta (elem-meta e :shape)}
    :ctr     {:type :ctr :key (elem-key e :ctr)  :meta (elem-meta e :ctr)}
    :conn    {:type :conn :key (elem-key e :conn) :meta (elem-meta e :conn)}))


(defn- contained-elements
  [c]
  (filter vector? c))


(defn- mapv'
  "A version of mapv which keeps counter of the posn of the item in coll. The
   function f is a function whose first argument is the counter and second
   the item."
  [f coll]
  (second
   (reduce
    (fn [[counter acc] cur]
      [(inc counter) (conj acc (f counter cur))])
    [0 []]
    coll)))


(defn- disassemble-elem
  ([e] (disassemble-elem e []))
  ([e posn]
   (case (elem-type e)
     :ctr    [(-> (describe-elem e) (assoc :posn posn))
              (mapv' #(disassemble-elem %2 (conj posn %1)) (contained-elements e))]
     (-> (describe-elem e) (assoc :posn posn)))))


(defn disassemble
  "Takes a collection of  dictim elements, including nested elements and
   returns a sequence of maps where each map represents one element.
   Each map has the same keys:
     :type     element type, :shape/:cmt/:attrs/:conn/:ctr
     :key      the element's key. In the case of :cmt, the comment string itself.
               In the case of :attrs, the hash of the attribute map
     :meta     contains a map of :label and :attrs
     :posn     a vector of integers used to indicate the element's position within
               the original collection of (nested) elements. :posn is used in
               (re)assembling the sequence of maps into dictim later."
  [elems]
  (flatten (mapv' #(disassemble-elem %2 [%1]) elems)))



;; assembling


(defn rnil [coll]
  (into [] (remove nil? coll)))


(defn assemble-other
  [m]
  (case (:type m)
    :shape    (rnil [(:key m) (-> m :meta :label) (-> m :meta :attrs)])
    :cmt      [:comment (:key m)]
    :attrs    (:key m)
    :conn     (rnil (conj (:key m) (-> m :meta :label) (-> m :meta :attrs)))))


(declare assemble)


(defn assemble-ctr
  [coll]
  ;; we know the first is the ctr the rest are it's children
  (let [[ctr & items] coll]
    (into [] (concat
              (rnil [(:key ctr) (-> ctr :meta :label) (-> ctr :meta :attrs)])
              (assemble items)))))


(defn trim-posn [item]
  (update-in item [:posn] rest))


(defn trim-posns [items]
  (map trim-posn items))


(defn posn-comparator
  "A custom comparator for sorting position vectors."
  [p1 p2]
  (let [c1 (count p1)
        c2 (count p2)
        shortest (min c1 c2)
        p1' (into [] (take shortest p1))
        p2' (into [] (take shortest p2))]
    (if (and (= p1' p2') (> c2 c1))
      -1
      (compare p1' p2'))))


(def sort-by-posn (partial sort-by :posn posn-comparator))


(defn assemble
  "Assembles a piece of flat dictim into dictim."
  [elems]
  (mapv
   (fn [[posn items]]
     (let [items (trim-posns items)]
       (case (-> items first :type) ;; could differentiate by count
         :ctr       (assemble-ctr items)
         (assemble-other (first items)))))
   (group-by (comp first :posn) (sort-by-posn elems))))



; utils for working with flat dictim

(defn- split-at-posn
  "Returns a vector of [(take-while comparator -1) (the rest)]"
  ([posn elems] (split-at-posn posn elems false))
  ([posn elems after?]
   (let [[pre post]
         (reduce
          (fn [[befores afters] cur]
            (if (< (posn-comparator (:posn cur) posn) 0)
              [(conj befores cur) afters]
              [befores (conj afters cur)]))
          [[] []]
          elems)]
     (if after?
       [(conj pre (first post)) (into [] (rest post))]
       [pre post]))))


(defn- key->posn
  [k elems]
  (if-let [posn (-> (filter (fn [item] (= k (:key item))) elems)
                    first
                    :posn)]
    posn
    (throw (error (str "Could not find element with key " k " or retrieve a position"
                       " from it.")))))


(defn- posn->elem
  [p elems]
  (if-let [elem (-> (filter (fn [item] (= p (:posn item))) elems)
                    first)]
    elem
    (throw (error (str "Could not find element with :posn " p)))))


(defn- loc->posn
  [loc elems]
  (cond
    (vector? loc)       (when (posn->elem loc elems) loc)
    :else               (key->posn loc elems)))


(defn insert
  "Inserts a flat-dictim element into a sequence of flat-dictim elements (elems)
   at the point dictated by its :posn vector. Updates the :posn vector of affected
   elements after the insertion point. elems does not need to be pre-sorted.
   If the :posn vector of elem matches an element in elems, then elem is
   inserted just before that point."
  [elem elems & {:keys [before after]
                 :or {before nil after nil}}]

  (when (and before after)
    (throw (error "Can only specify :before or :after for insertions, not both.")))

  (if-let [posn (cond
                  before        (loc->posn before elems)
                  after         (loc->posn after elems)
                  :else         (:posn elem))]

    (let [[pre post] (split-at-posn posn (sort-by-posn elems) after)
          len (count posn)
          posn (if after (update posn (dec len) inc) posn)]

      (into pre
       (cons (assoc elem :posn posn)
             (map
              (fn [item]
                (if (>= (count (:posn item)) len)
                  (update-in item [:posn (dec len)] inc)
                  item))
              post))))

    (throw (error (str "No position specified for insertion of elem " elem)))))


(defn delete
  "Deletes a flat-dictim element from a sequence of flat-dictim elements (elems)
   at the point specified either by a :posn vector or it's key. Subsquent elements
   have their :posn vector updated appropriately. elems does not need to be pre-sorted.
   note: deleting a :ctr element will also delete the contained elements."
  [id elems]
  (let [elems (sort-by-posn elems)
        posn (loc->posn id elems)
        elem (posn->elem posn elems)
        ctr? (= :ctr (:type elem))
        [pre post] (split-at-posn posn elems)
        len (count posn)]
           
    (into pre
          (if ctr?
            
            (reduce
             (fn [acc item]
               (cond
                 (= (seq posn) (take len (:posn item))) ;; contained element
                 acc

                 (= (butlast posn) (take (dec len) (:posn item))) ;; sibling element
                 (concat acc [(update-in item [:posn (dec len)] dec)])

                 :else (concat acc [item])))
             []
             (rest post))
            
            (map
             (fn [item]
               (if (>= (count (:posn item)) len)
                 (update-in item [:posn (dec len)] dec)
                 item))
             (rest post))))))
