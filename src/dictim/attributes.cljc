(ns
    ^{:author "judepayne"
      :doc "Namespace for handling d2 reserved keywords."}
    dictim.attributes
  (:require [dictim.utils :refer [try-parse-primitive] :rename {try-parse-primitive tpp}]))

;; keep all d2 reserved keywords in one place to make updating easier

(def shapes #{"rectangle" "square" "page" "parallelogram" "document" "cylinder" "queue" "package" "step" "callout" "stored_data" "person" "diamond" "oval" "circle" "hexagon" "cloud" "sql_table" "class" "sequence_diagram" "text"})


(defn- dekey [k]
  (if (keyword? k) (name k) k))


(def d2-attributes-map
  {"shape" {:validate-fn (fn [v] (contains? shapes (dekey v)))}
   "label" {:validate-fn (fn [v] true)}
   "source-arrowhead" {:validate-fn (fn [v] true)}
   "target-arrowhead" {:validate-fn (fn [v] true)}
   "style" {:validate-fn (fn [v] true)}
   "near" {:validate-fn (fn [v] true)}
   "icon" {:validate-fn (fn [v] true)}
   "width" {:validate-fn (fn [v] true)}
   "height" {:validate-fn (fn [v] true)}
   "constraint" {:validate-fn (fn [v] true)}
   "direction" {:validate-fn (fn [v] true)}
   "opacity" {:validate-fn (fn [v] (let [v (tpp v)]
                                     (and (or (float? v) (integer? v))
                                          (>= v 0)
                                          (<= 0 v ))))}
   "fill" {:validate-fn (fn [v] true)}
   "stroke" {:validate-fn (fn [v] true)}
   "stroke-width" {:validate-fn (fn [v] true)}
   "stroke-dash" {:validate-fn (fn [v] true)}
   "border-radius" {:validate-fn (fn [v] true)}
   "font-color" {:validate-fn (fn [v] true)}
   "shadow" {:validate-fn (fn [v] true)}
   "multiple" {:validate-fn (fn [v] true)}
   "3d" {:validate-fn (fn [v] true)}
   "animated" {:validate-fn (fn [v] true)}
   "link" {:validate-fn (fn [v] true)}
   "font-size" {:validate-fn (fn [v] true)}
   "tooltip" {:validate-fn (fn [v] true)}
   "filled" {:validate-fn (fn [v] true)}
   "italic" {:validate-fn (fn [v] true)}
   "bold" {:validate-fn (fn [v] true)}
   "double-border" {:validate-fn (fn [v] true)}
   "underline" {:validate-fn (fn [v] true)}
   "font" {:validate-fn (fn [v] true)}
   "fill-pattern" {:validate-fn (fn [v] true)}
   "class" {:validate-fn (fn [v] true)}
   "grid-rows" {:validate-fn (fn [v] (integer? (tpp v)))}
   "grid-columns" {:validate-fn (fn [v] (integer? (tpp v)))}
   "text-transform" {:validate-fn (fn [v] true)}})


(defn validate-fn [d2-keyword]
  (:validate-fn (get d2-attributes-map d2-keyword)))


(def d2-attributes (into #{} (keys d2-attributes-map)))


(defn d2-keyword?
  [w]
  (contains? d2-attributes (name w)))


(defn d2-keys []
  (apply str (interpose "|" (map #(str "'" % "'") d2-attributes))))
