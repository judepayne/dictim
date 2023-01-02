(ns dictim.attributes)

;; a namespace to hold the set of reserved d2 keywords used as attributes.

(def d2-attributes
  #{"shape" "label" "source-arrowhead" "target-arrowhead" 
    "style" "near" "icon" "width" "height" "constraint" 
    "direction" "opacity" "fill" "stroke" "stroke-width" 
    "stroke-dash" "border-radius" "font-color" "shadow" 
    "multiple" "3d" "animated" "link"})


(defn d2-keyword?
  [w]
  (contains? d2-attributes (name w)))


(defn d2-keys []
  (apply str (interpose "|" (map #(str "'" % "'")d2-attributes))))
