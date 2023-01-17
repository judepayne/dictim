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
    "filled"})


(defn d2-keyword?
  [w]
  (contains? d2-attributes (name w)))


(defn d2-keys []
  (apply str (interpose "|" (map #(str "'" % "'")d2-attributes))))


(defn d2-keys-r []
  (apply str (interpose "|" d2-attributes)))
