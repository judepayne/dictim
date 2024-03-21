(ns
    ^{:author "judepayne"
      :doc "Namespace for handling d2 reserved keywords."}
    dictim.attributes)

;; keep all d2 reserved keywords in one place to make updating easier

(def d2-attributes
  #{"shape" "label" "source-arrowhead" "target-arrowhead" 
    "style" "near" "icon" "width" "height" "constraint" 
    "direction" "opacity" "fill" "stroke" "stroke-width" 
    "stroke-dash" "border-radius" "font-color" "shadow" 
    "multiple" "3d" "animated" "link" "font-size" "tooltip"
    "filled" "italic" "bold" "double-border" "underline" "font"
    "fill-pattern"})


(def d2-attributes-map
  {"shape" {:validate-fn (fn [v] true)}
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
   "opacity" {:validate-fn (fn [v] true)}
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
   "class" {:validate-fn (fn [v] true)}})


(defn validate-fn [d2-keyword]
  (:validate-fn (get d2-attributes-map d2-keyword)))


(def d2-attributes (into #{} (keys d2-attributes-map)))


(defn d2-keyword?
  [w]
  (contains? d2-attributes (name w)))


(defn d2-keys []
  (apply str (interpose "|" (map #(str "'" % "'") d2-attributes))))
