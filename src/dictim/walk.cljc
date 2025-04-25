(ns dictim.walk
  (:require [dictim.utils :refer [direction? elem-type take-til-last]]))


(defn- rnil [coll]
  (into [] (remove nil? coll)))


(defn walk
  "Similar to clojure.walk/walk but refined for dictim. Only walks into
   children of elements that have children (e.g. :elements/:list/:ctr
    and the top level of attrs"
  [inner outer element]
  (case (elem-type element)
    :elements   (outer (apply list (map inner element)))
    
    :ctr        (let [[nc c] (split-with (complement vector?) element)
                      [non-ats ats] (split-with (complement map?) nc)]
                  (outer (rnil
                          (if (not (empty? ats))
                            (apply conj (into [] non-ats)
                                   (inner ats)
                                   (mapv inner c))
                            (apply conj (into [] non-ats)
                                   (mapv inner c))))))
        
    :list       (outer (into [] (cons (first element) (map inner (rest element)))))
    
    :shape      (let [[not-ats ats] (split-with (complement map?) element)]
                  (outer (rnil
                          (apply conj (into [] not-ats)
                                 (mapv inner ats)))))

    :conn       (if (some map? element)
                  (let [front (into [] (take-while (complement map?) element))
                        attrs (last element)]
                    (outer (conj front (inner attrs))))
                  (outer element))

    :conn-ref   (if (some map? element)
                  (let [front (into [] (take-while (complement map?) element))
                        attrs (last element)]
                    (outer (conj front (inner attrs))))
                  (outer element))

    :attrs      (outer element)   ;; for directives/ top-level attrs

    (outer element)))


(defn postwalk
  "Similar to clojure.walk/postwalk but for dictim."
  [f element]
  (walk (partial postwalk f) f element))


(defn prewalk
  "Similar to clojure.walk/prewalk but for dictim."
  [f element]
  (walk (partial prewalk f) identity (f element)))


(defn- ->keyword [x]
  (cond
    (and (string? x)
         (clojure.string/includes? x " "))  x

    :else (keyword x)))


(defn- update-attrs-keys [f m]
  (clojure.walk/postwalk
   (fn [x]
     (if (map? x)
       (into {} (map (fn [[k v]] [(f k) v]) x))
       x))
   m))


(defn- update-key [f element]
  (case (elem-type element)
    :quikshape  (f element)

    :shape      (into [] (cons (f (first element)) (rest element)))

    :ctr        (into [] (cons (f (first element)) (rest element)))

    :conn-ref   (into []
                      (concat [(f (first element))
                               (second element)
                               (f (nth element 2))]
                              (drop 3 element)))

    :conn       (let [[ds nds] (take-til-last direction? element)
                      ds (conj (into [] ds) (first nds))
                      start (reduce
                             (fn [acc cur]
                               (conj acc
                                     (if (direction? cur)
                                       cur
                                       (f cur))))
                             []
                             ds)]
                  (into [] (concat start (rest nds))))

    :list        (into [] (cons (f (first element)) (rest element)))

    :attrs       (update-attrs-keys f element)

    element))


(defn keywordize-keys
  "keywordizes keys in the supplied dictim elements, including attr keys.
   key strings with spaces are not keywordized."
  [& elems]
  (let [elems (reverse (into () elems))]
    (map (partial postwalk (partial update-key ->keyword)) elems)))


(defn stringify-keys
  "converts keys to strings in the supplied dictim elements, including attr keys."
  [& elems]
  (let [elems (reverse (into () elems))]
    (map (partial postwalk (partial update-key name)) elems)))
