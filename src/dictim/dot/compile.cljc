(ns ^{:Author "Jude Payne"
      :doc "Namespace for compiling dictim to graphviz' dot"}
    dictim.dot.compile
  (:require [clojure.string :as str]
            [dictim.format :as f]
            [dictim.utils :refer [kstr? direction? take-til-last elem-type shape? error]]
            [dictim.validate :refer [all-valid?]])
  (:refer-clojure :exclude [list]))


(defn- err [msg]
  (throw (error (str msg " is invalid."))))

;; is the graph directed
(def ^:private directed? (atom false))


;; graph level directives can be appended to during compilation
(def ^:private directives (atom nil))


;; collect containers during compilation
(def ^:private relationships (atom nil))


;; current container
(def ^:private breadcrumbs (atom nil))


(defn- containers [] (set (keys @relationships)))


(defn- container? [id]
  (contains? (containers) id))


(defn- reset-atoms! []
  (reset! breadcrumbs nil)
  (reset! directives nil)
  (reset! relationships nil)
  (reset! directed? nil))


;; TODO dev function - delete
(defn- atoms []
  (println "directives " @directives)
  (println "relationships " @relationships)
  (println "breadcrumbs " @breadcrumbs)
  (println "directed?" @directed?))

;; pre-pass to collect state required later in compilation

(defmulti ^:private pre-pass elem-type)


(defmethod pre-pass :attrs [m]
  (swap! directives merge m))


(defmethod pre-pass :shape [_] nil)


(defmethod pre-pass :quickshape [_] nil)


(defmethod pre-pass :conn [_] nil)


(defmethod pre-pass :cmt [_] nil)


(defmethod pre-pass :list [li]
  (map pre-pass (rest li)))


(defmethod pre-pass :empty-lines [_] nil)


(defmethod pre-pass :ctr [el]
  (let [contained (filter vector? el)
        shapes (filter shape? contained)]
      (swap! breadcrumbs conj (first el))
    (when (seq shapes)
      (swap! relationships merge {(first el) (map first shapes)}))
    (mapv pre-pass contained)
    (swap! breadcrumbs butlast)))


;; --------------------------------------------------------------------------
;; compilation


(def ^:private escapable-characters "\\|{}\"")


(defn- escape-string
  "Escape characters that are significant for the dot format."
  [s]
  (reduce
   #(str/replace %1 (str %2) (str "\\" %2))
   s
   escapable-characters))


(defn- wrap-string [s] (str "\"" s "\""))


(defn- cluster [c] (str "cluster_" (str/replace (name c) #" " "_")))


(def ^:private ^:dynamic sep ";\n")


;; -----
;; attrs

(defn- attr-pairs
  "creates a list of attr pairs."
  [m]
  (map
   (fn [[k v]]
     (cond
       (and (string? v) (clojure.string/includes? v " "))
       (str (name k) "=" (wrap-string (escape-string v)))

       (and (= "style" (name k)) (map? v))
       (let [styles (keys (into {} (filter (fn [[_ v]] (true? v)) v)))]
         (str "style=" (wrap-string (apply str (interpose "," (map name styles))))))

       :else (str (name k) "=" (wrap-string v))))
   m))


(defn- attrs-raw
  "layout a map of attrs."
  [brackets? separator m]
  (when m
    (str
     (when brackets? "[")
     (apply str (interpose separator (attr-pairs m)))
     (when brackets? "]"))))


(def ^:private attrs (partial attrs-raw false)) ;; can't close over 'sep' as dynamic


(def ^:private attrs-elem (partial attrs-raw true ","))

;; -----------
;; connections

(defn- optionals
  "opts is a vector of label and attrs - both optional. converts to string."
  [[maybe-label attrs]]
  (cond
    attrs                  (assoc attrs :label maybe-label)
    (string? maybe-label)  {:label maybe-label}
    :else maybe-label))


;; The edge logic for Graphviz is quite complicated
(defn- edge
  "Creates an edge"
  [item1 dir item2 opts]
  (let [backwards? (= "<-" dir)
        n1 (if backwards? item2 item1)
        n2 (if backwards? item1 item2)
        ctr1? (container? n1)
        node1 (if ctr1? (rand-nth (get @relationships n1)) n1)
        ctr2? (container? n2)
        node2 (if ctr2? (rand-nth (get @relationships n2)) n2)]

    (if (not (and node1 node2))
      (err "In Graphviz, container edges must be between containers that both contain shapes.")
      
      (let [ats (merge
                 (when ctr1? {:ltail (cluster n1)})
                 (when ctr2? {:lhead (cluster n2)})
                 (when (= dir "--") {:dir "none"})
                 (when (= dir "<->") {:dir "both"})
                 (optionals opts))]

        ;; For container edges, compound must be set true on the graph
        (when (or ctr1? ctr2?) (swap! directives assoc :compound true))
        (when (contains? #{"<-" "<->" "->"} dir) (reset! directed? true))
        (str (name node1) " -> " (name node2) (when ats (str " " (attrs-elem ats))) sep)))))


(defn- single-conn
  "layout connection vector."
  [[k1 dir k2 & opts]]
  (edge k1 dir k2 opts))


(defn- no-containers?
  "True if there are no containers in the collection of nodes."
  [nodes]
  (reduce (fn [acc cur]
            (and acc (not (container? cur))))
          true
          nodes))


(defn- same?
  "Are all the items in (non empty) coll the same?"
  [coll]
  (if (seq coll)
    (apply = coll)
    false))


(defn- ->single-edges [shapes dirs]
  (let [terms (partition 2 1 shapes)]
    (map-indexed
     (fn [idx item]
       [(first item) (nth dirs idx) (second item)])
     terms)))


(defn- multi-conn
  "layout multiple connection vector."
  [c]
  (let [[kds [lk & opts]] (take-til-last direction? c)
        shapes (take-nth 2 (conj (vec kds) lk))
        dirs (take-nth 2 (rest kds))
        dir  (first dirs)]
    (if (and (same? dirs) (no-containers? shapes))

      ;; Can be laid out on a line
      (let [nodes (if (= "<-" dir) (reverse shapes) shapes)
            conn-part (apply str (interpose " -> " (map name nodes)))
            ats (merge
                 (when (= dir "--") {:dir "none"})
                 (when (= dir "<->") {:dir "both"})
                 (optionals opts))]

        (when (contains? #{"<-" "<->" "->"} dir) (reset! directed? true))

        (str conn-part (when ats (str " " (attrs-elem ats))) sep))

      ;; too much variation to be laid out on a line
      (apply str
        (map
         (fn [e] (single-conn (concat e opts)))
         (->single-edges shapes dirs))))))


;; layout

(defmulti ^:private layout elem-type)


;; suppress attrs layout, as graph level attrs collected in directives
(defmethod layout :attrs [_] )


(defmethod layout :shape [[k & opts]]
  (str (name k) (when (seq opts) (-> opts optionals attrs-elem)) sep))


(defmethod layout :quickshape [k]
  (str (name k) sep))


(defmethod layout :conn [c]
  (let [num-dirs (count (filter direction? c))]
    (if (> num-dirs 1)
      (multi-conn c)
      (single-conn c))))


(defmethod layout :cmt [c]
  (str "/*" (second c) "*/" sep))


(defmethod layout :list [li]
  (str
   (binding [sep ";"]
     (apply str (map layout (rest li))))
   sep))


(defmethod layout :empty-lines [[em c]]
  (apply (str (repeat c "\n"))))


(defn- split-contained [opts]
  [(filter (complement vector?) opts) (filter vector? opts)])


(defmethod layout :ctr [[k & opts]]
  (let [[opts elems] (split-contained opts)]
    (str "subgraph "
         (cluster k)
         " {\n"
         (when (seq opts) (str (attrs sep (optionals opts)) sep))
         (apply str
                (map layout elems))
         "}\n")))


(defn dot
  "Converts dictim elements to a (formatted) dot string.
   Validates each element, throws an error if invalid."
  [& elems]
  (all-valid? elems :dot)
  
  (reset-atoms!)
  (run! pre-pass elems)

  (let [dot (apply str (mapcat layout elems))
        options (attrs sep @directives)
        g (if @directed? "digraph" "graph")]

    (f/fmt
     (str g " {\n" (when options (str options sep)) dot "}")
     :tab 2)))
