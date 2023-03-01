(ns dictim.alpha.translate
  ^{:Author "Jude Payne"
    :doc "Namespace for translating d2 flavour dictim to dot and visa versa."}
  (:require [clojure.string :as str]
            [clojure.walk :refer [walk]]
            [dictim.attributes :as d2ats]))


;; This is `alpha` functionality. d2 and graphviz cannot be made to be equivalent.
;; Although both are graph layout and rendering engines, the control over layout
;; and rendering varies quite a bit. A conversion of d2 attrs to dot attrs (and
;; visa versa) is an inherently lossy process.


;; from d2 to dot *********************************************************************

(def ^:private d2->dot-arrowheads
  {"triangle" "normal"
   "arrow" "open"
   "diamond" "diamond"
   "circle" "dot"
   "cf-one" "odot"
   "cf-many" "crow"})


(def ^:private d2->dot-shapes
  {"square" :square
   "page" :note
   "parallelogram" :parallelogram
   "document" :note
   "cylinder" :cylinder
   "package" :box3d
   "step" :cds
   "diamond" :diamond
   "oval" :oval
   "circle" :circle
   "hexagon" :hexagon
   "cloud" :egg})


(def ^:private d2->dot-directions
  {"up" :BT
   "down" :TB
   "right" :LR
   "left" :RL})


(defn- merge-submap [parent-key submap mappings]
  (reduce
   (fn [acc [k v]]
     (let [new-k (str parent-key "." (name k))]
       (if (map? v)
         (merge-submap new-k v mappings)
         (if-let [f (get mappings new-k)]
           (merge acc (f v))
           acc))))
   nil
   submap))


(def ^:private d2->dot
  ;; (fn [_] nil) means there's no natural Graphviz equiv., so filter out.
  ;; Restricted to vanilla graphviz. No record-based nodes or html-like labels
  {"shape" (fn [v] {:shape (get d2->dot-shapes (name v) :rectangle)})
   "source-arrowhead" (fn [style-map] (merge-submap "source-arrowhead" style-map d2->dot))
   "source-arrowhead.shape" (fn [v] (get d2->dot-arrowheads v "normal"))
   "source-arrowhead.style.fill" (fn [_] nil)
   "target-arrowhead" (fn [style-map] (merge-submap "source-arrowhead" style-map d2->dot))
   "target-arrowhead.shape" (fn [v] (get d2->dot-arrowheads v "normal"))
   "target-arrowhead.style.fill" (fn [_] nil)
   "near" (fn [_] nil)
   "icon" (fn [v] {:image v}) ;; where v is a path to an image file
   "constraint" (fn [_] nil) ;; in d2, constraint is only to do with sql_tables, not rank.
   "direction" (fn [v] {:rankdir (get d2->dot-directions (name v))})
   "font" (fn [_] nil)
   
   ;; style and sub-keys
   "style" (fn [style-map] (merge-submap "style" style-map d2->dot))
   "style.opacity" (fn [_] nil)
   "style.stroke" (fn [v] {:color v})
   "style.fill" (fn [v] {:style "filled" :fillcolor v})
   "style.stroke-width" (fn [v] {:penwidth v})
   "style.stroke-dash" (fn [_] nil)
   "style.border-radius" (fn [v] (when (> v 0) {:style "rounded"}))
   "style.shadow" (fn [_] nil)
   "style.3d" (fn [_] nil)
   "style.multiple" (fn [_] nil)
   "style.font-size" (fn [v] {:fontsize v}) ;; do we need a translation of the value?
   "style.font-color" (fn [v] {:fontcolor v})
   "style.animated" (fn [_] nil)
   "style.bold" (fn [_] nil)
   "style.italic" (fn [_] nil)
   "style.underline" (fn [_] nil)})


(defn- d2-key-part
  "Converts a qualified key, k into just the d2 parts."
  [k]
  (let [parts (str/split (name k) #"\.")]
    (str/join "." (filter d2ats/d2-keyword? parts))))


(defn- merge-dot [k v1 v2]
  (case k
    "style" (str v1 "," v2)
    (last v2)))


;; from dot to d2 *********************************************************************

(def ^:private dot->d2
  {:style (fn [k v] {k 2})})




;; translation  ***********************************************************************


(def ^:private translations
  {[:d2 :dot] {:key-preprocessor name
               :mappings d2->dot
               :val-postprocessor merge-dot}
   [:dot :d2] {:key-preprocessor identity
               :mappings dot->d2
               :val-postprocessor identity}})


(defn- merge-with-key
  "Like merge-with but f is passed k as well."
  [f & maps]
  (when (some identity maps)
    (let [merge-entry (fn [m e]
			(let [k (key e) v (val e)]
			  (if (contains? m k)
			    (assoc m k (f k (get m k) v))
			    (assoc m k v))))
          merge2 (fn [m1 m2]
		   (reduce merge-entry (or m1 {}) (seq m2)))]
      (reduce merge2 maps))))


(defn- deep-walk
  "Like clojure.walk/walk but walks into forms."
  [inner outer form]
  (walk (partial deep-walk inner outer) outer (inner form)))


(defn- prune? [item]
  (or (nil? item) (and (map? item) (empty? item))))


(defn translate
  "Translates dictim from flavour :from to flavour :to"
  [dict from to]
  (let [{prep-key :key-preprocessor
         mappings :mappings
         merge-fn :val-postprocessor}
        (get translations [from to])]
    (deep-walk
     (fn [form]
       (if (map? form)
         (reduce
          (fn [m [k v]]
            (let [k (prep-key k)
                  f (get mappings k)]
              (if f
                (merge-with-key merge-fn m (f v))
                (assoc m k v)))) ;; attrs with no mapping are retained.
          nil
          form)
         form))
     (fn [form] ;; remove any maps left empty after translation
       (cond
         (list? form)
         (remove prune? form)

         (vector? form)
         (into [] (remove prune? form))
                  
         :else form))
     dict)))
