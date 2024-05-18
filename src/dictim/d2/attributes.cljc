(ns
    ^{:author "judepayne"
      :doc "Namespace for handling d2 reserved keywords."}
    dictim.d2.attributes
  (:require [dictim.utils
             :refer [try-parse-primitive kstr?]
             :rename {try-parse-primitive tpp}]
            [clojure.string :as str]))

;; keep all d2 reserved keywords in one place to make updating easier

(def ^:private shapes #{"rectangle" "square" "page" "parallelogram" "document" "cylinder" "queue" "package" "step" "callout" "stored_data" "person" "diamond" "oval" "circle" "hexagon" "cloud" "sql_table" "class" "sequence_diagram" "text"})


(def ^:private css-color-names
  #{"ORANGE" "MEDIUMPURPLE" "DARKGRAY" "BURLYWOOD" "PURPLE" "SNOW" "PINK" "DARKORCHID" "ROSYBROWN" "GRAY" "KHAKI" "DARKGOLDENROD" "SPRINGGREEN" "TOMATO" "PALETURQUOISE" "DIMGRAY" "BLUEVIOLET" "LIGHTGOLDENRODYELLOW" "STEELBLUE" "ORANGERED" "POWDERBLUE" "LINEN" "LIGHTSTEELBLUE" "LIGHTSLATEGRAY" "SEAGREEN" "GAINSBORO" "DARKGREEN" "WHEAT" "INDIANRED" "CRIMSON" "FLORALWHITE" "HOTPINK" "VIOLET" "BISQUE" "LIGHTSALMON" "BLANCHEDALMOND" "GOLDENROD" "AZURE" "MIDNIGHTBLUE" "MEDIUMTURQUOISE" "LIGHTPINK" "CHOCOLATE" "AQUAMARINE" "SILVER" "GHOSTWHITE" "NAVY" "TURQUOISE" "LEMONCHIFFON" "DEEPSKYBLUE" "LAVENDER" "PERU" "DARKRED" "MAGENTA" "DARKMAGENTA" "DARKBLUE" "LIGHTCORAL" "CORNFLOWERBLUE" "DARKTURQUOISE" "DARKSLATEGRAY" "PALEGOLDENROD" "REBECCAPURPLE" "LIME" "MEDIUMSPRINGGREEN" "SIENNA" "DARKVIOLET" "FORESTGREEN" "LAVENDERBLUSH" "GREEN" "BROWN" "LIGHTSEAGREEN" "SKYBLUE" "LIGHTGRAY" "LIGHTCYAN" "BLUE" "OLIVEDRAB" "MEDIUMORCHID" "LIGHTSKYBLUE" "MISTYROSE" "INDIGO" "PALEVIOLETRED" "MINTCREAM" "DARKSLATEBLUE" "WHITESMOKE" "CADETBLUE" "LIGHTBLUE" "HONEYDEW" "MEDIUMSEAGREEN" "YELLOWGREEN" "IVORY" "MAROON" "SLATEGRAY" "DODGERBLUE" "SADDLEBROWN" "MEDIUMAQUAMARINE" "THISTLE" "DARKCYAN" "CORAL" "ORCHID" "CORNSILK" "OLDLACE" "AQUA" "LAWNGREEN" "NAVAJOWHITE" "WHITE" "OLIVE" "ANTIQUEWHITE" "MEDIUMSLATEBLUE" "LIGHTYELLOW" "SANDYBROWN" "ALICEBLUE" "BEIGE" "DEEPPINK" "LIGHTGREEN" "DARKSEAGREEN" "PEACHPUFF" "TAN" "PAPAYAWHIP" "LIMEGREEN" "MEDIUMVIOLETRED" "PALEGREEN" "TEAL" "GREENYELLOW" "SALMON" "GOLD" "MEDIUMBLUE" "DARKORANGE" "SEASHELL" "PLUM" "DARKOLIVEGREEN" "DARKKHAKI" "YELLOW" "BLACK" "MOCCASIN" "ROYALBLUE" "RED" "CYAN" "CHARTREUSE" "DARKSALMON" "SLATEBLUE" "FUCHSIA"})


(defn- hex-color? [s] ;;must be quoted
  (re-matches #"^('|\")#?([a-fA-F0-9]{6}|[a-fA-F0-9]{3})('|\")$" s))


(defn- d2-class-ref? [s]
  (re-matches #"\$\{.*\}" s))


(defn- valid-color? [s]
  (boolean
   (or (hex-color? s)
       (contains? css-color-names (str/upper-case s))
       (d2-class-ref? s))))


(defn- dekey [k]
  (if (keyword? k) (name k) k))


(defn- int-between? [lower upper v]
  (let [v (tpp v)]
    (and (integer? v)
         (>= v lower)
         (<= v upper))))


(def d2-attributes-map
  {"shape" {:validate-fn (fn [v] (contains? shapes (dekey v)))}
   "label" {:validate-fn (fn [v] (kstr? v))}
   "source-arrowhead" {:validate-fn (fn [v] true)}
   "target-arrowhead" {:validate-fn (fn [v] true)}
   "style" {:validate-fn (fn [v] true)}
   "near" {:validate-fn (fn [v] (let [v (dekey v)]
                                  (contains?
                                   #{"top-left" "top-center" "top-right"
                                     "center-left" "center-right"
                                     "bottom-left" "bottom-center" "bottom-right"}
                                   v)))}
   "icon" {:validate-fn (fn [v] (string? v))}
   "width" {:validate-fn (fn [v] (integer? (tpp v)))}
   "height" {:validate-fn (fn [v] (integer? (tpp v)))}
   "constraint" {:validate-fn (fn [v] (string? v))}
   "direction" {:validate-fn (fn [v] (let [v (dekey v)]
                                       (contains?
                                        #{"up" "down" "left" "right"}
                                        v)))}
   "opacity" {:validate-fn (fn [v] (let [v (tpp v)]
                                     (and (or (float? v) (integer? v))
                                          (>= v 0)
                                          (<= 0 v ))))}
   "fill" {:validate-fn (fn [v] (or (nil? v) (valid-color? v)))}
   "stroke" {:validate-fn (fn [v] (or (nil? v) (valid-color? v)))}
   "stroke-width" {:validate-fn (partial int-between? 1 15)}
   "stroke-dash" {:validate-fn (partial int-between? 1 10)}
   "border-radius" {:validate-fn (partial int-between? 0 20)}
   "font-color" {:validate-fn (fn [v] (string? (dekey v)))}
   "shadow" {:validate-fn (fn [v] (boolean? (tpp v)))}
   "multiple" {:validate-fn (fn [v] (boolean? (tpp v)))}
   "3d" {:validate-fn (fn [v] (boolean? (tpp v)))}
   "animated" {:validate-fn (fn [v] (boolean? (tpp v)))}
   "link" {:validate-fn (fn [v] (string? v))}
   "font-size" {:validate-fn (partial int-between? 8 100)}
   "tooltip" {:validate-fn (fn [v] (string? v))}
   "filled" {:validate-fn (fn [v] (boolean? (tpp v)))}
   "italic" {:validate-fn (fn [v] (boolean? (tpp v)))}
   "bold" {:validate-fn (fn [v] (boolean? (tpp v)))}
   "double-border" {:validate-fn (fn [v] (boolean? (tpp v)))}
   "underline" {:validate-fn (fn [v] (boolean? (tpp v)))}
   "font" {:validate-fn (fn [v] (= "mono" (str v)))}
   "fill-pattern" {:validate-fn (fn [v] (let [v (dekey v)]
                                          (contains?
                                           #{"dots" "lines" "grain" "none"} v)))}
   "class" {:validate-fn (fn [v] (kstr? v))}
   "grid-rows" {:validate-fn (fn [v] (integer? (tpp v)))}
   "grid-columns" {:validate-fn (fn [v] (integer? (tpp v)))}
   "text-transform" {:validate-fn (fn [v] (let [v (dekey v)]
                                            (contains? #{"uppercase" "lowercase" "title" "none"} v)))}})


(defn validate-fn [d2-keyword]
  (:validate-fn (get d2-attributes-map d2-keyword)))


(def d2-attributes (into #{} (keys d2-attributes-map)))


(defn d2-keyword?
  [w]
  (contains? d2-attributes (name w)))


(defn d2-keys []
  (apply str (interpose "|" (map #(str "'" % "'") d2-attributes))))
