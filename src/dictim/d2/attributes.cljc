(ns
    ^{:author "judepayne"
      :doc "Namespace for handling d2 reserved keywords."}
    dictim.d2.attributes
  (:require [dictim.utils
             :refer [try-parse-primitive kstr? error]
             :rename {try-parse-primitive tpp}]
            [clojure.string :as str]))


(defn- err [msg]
  (throw (error msg)))

;; keep all d2 reserved keywords in one place to make updating easier


(def ^:private shapes #{"rectangle" "square" "page" "parallelogram" "document" "cylinder" "queue" "package" "step" "callout" "stored_data" "person" "diamond" "oval" "circle" "hexagon" "cloud" "sql_table" "class" "sequence_diagram" "text"})


(def ^:private css-color-names
  #{"ORANGE" "MEDIUMPURPLE" "DARKGRAY" "BURLYWOOD" "PURPLE" "SNOW" "PINK" "DARKORCHID" "ROSYBROWN" "GRAY" "KHAKI" "DARKGOLDENROD" "SPRINGGREEN" "TOMATO" "PALETURQUOISE" "DIMGRAY" "BLUEVIOLET" "LIGHTGOLDENRODYELLOW" "STEELBLUE" "ORANGERED" "POWDERBLUE" "LINEN" "LIGHTSTEELBLUE" "LIGHTSLATEGRAY" "SEAGREEN" "GAINSBORO" "DARKGREEN" "WHEAT" "INDIANRED" "CRIMSON" "FLORALWHITE" "HOTPINK" "VIOLET" "BISQUE" "LIGHTSALMON" "BLANCHEDALMOND" "GOLDENROD" "AZURE" "MIDNIGHTBLUE" "MEDIUMTURQUOISE" "LIGHTPINK" "CHOCOLATE" "AQUAMARINE" "SILVER" "GHOSTWHITE" "NAVY" "TURQUOISE" "LEMONCHIFFON" "DEEPSKYBLUE" "LAVENDER" "PERU" "DARKRED" "MAGENTA" "DARKMAGENTA" "DARKBLUE" "LIGHTCORAL" "CORNFLOWERBLUE" "DARKTURQUOISE" "DARKSLATEGRAY" "PALEGOLDENROD" "REBECCAPURPLE" "LIME" "MEDIUMSPRINGGREEN" "SIENNA" "DARKVIOLET" "FORESTGREEN" "LAVENDERBLUSH" "GREEN" "BROWN" "LIGHTSEAGREEN" "SKYBLUE" "LIGHTGRAY" "LIGHTCYAN" "BLUE" "OLIVEDRAB" "MEDIUMORCHID" "LIGHTSKYBLUE" "MISTYROSE" "INDIGO" "PALEVIOLETRED" "MINTCREAM" "DARKSLATEBLUE" "WHITESMOKE" "CADETBLUE" "LIGHTBLUE" "HONEYDEW" "MEDIUMSEAGREEN" "YELLOWGREEN" "IVORY" "MAROON" "SLATEGRAY" "DODGERBLUE" "SADDLEBROWN" "MEDIUMAQUAMARINE" "THISTLE" "DARKCYAN" "CORAL" "ORCHID" "CORNSILK" "OLDLACE" "AQUA" "LAWNGREEN" "NAVAJOWHITE" "WHITE" "OLIVE" "ANTIQUEWHITE" "MEDIUMSLATEBLUE" "LIGHTYELLOW" "SANDYBROWN" "ALICEBLUE" "BEIGE" "DEEPPINK" "LIGHTGREEN" "DARKSEAGREEN" "PEACHPUFF" "TAN" "PAPAYAWHIP" "LIMEGREEN" "MEDIUMVIOLETRED" "PALEGREEN" "TEAL" "GREENYELLOW" "SALMON" "GOLD" "MEDIUMBLUE" "DARKORANGE" "SEASHELL" "PLUM" "DARKOLIVEGREEN" "DARKKHAKI" "YELLOW" "BLACK" "MOCCASIN" "ROYALBLUE" "RED" "CYAN" "CHARTREUSE" "DARKSALMON" "SLATEBLUE" "FUCHSIA"})


(def ^:private transparent #{"TRANSPARENT"})


(def ^:private re-hex-string "#?([a-fA-F0-9]{6}|[a-fA-F0-9]{3})")

(def ^:private re-hex (re-pattern (str "^('|\")" re-hex-string "('|\")$")))


(defn- hex-color? [s] ;;must be quoted
  (re-matches re-hex s))


#_(def ^:private re-linear-gradient-string
  (str "^('|\")linear-gradient\\(" re-hex-string ", *" re-hex-string"\\)('|\")$"))
(def ^:private re-linear-gradient-string (str "^('|\")linear-gradient\\(.*\\)('|\")$"))

(def ^:private re-linear-gradient (re-pattern re-linear-gradient-string))


(defn- linear-gradient? [s] ;;whole string must be quoted
  (re-matches re-linear-gradient s))


(defn- d2-class-ref? [s]
  (re-matches #"\$\{.*\}" s))


(defn- valid-color? [s]
  (boolean
   (or (hex-color? s)
       (linear-gradient? s)
       (contains? css-color-names (str/upper-case s))
       (contains? transparent (str/upper-case s))
       (d2-class-ref? s))))


(defn- dekey [k]
  (if (keyword? k) (name k) k))


(defn- int-between? [lower upper v]
  (let [v (tpp v)]
    (and (integer? v)
         (>= v lower)
         (<= v upper))))


;; ** How 'contexts' work **
;; style etc. at nil/ under source-arrowhead/ target-arrowhead/ */  ** => [nil source-a....]
;; style attrs -> under style  [:last "style"]
;; filled: under src/tgt then a style

(def ^:private star-ctx [#{nil "source-arrowhead" "target-arrowhead"}])
(def ^:private top-level-ctx [#{nil "source-arrowhead" "target-arrowhead" "*" "**"}])
(def ^:private style-ctx [[:last "style"]])


(declare ^:private style-attrs)


(def d2-attributes-map
  {"*" {:context star-ctx :validate-fn (constantly true)}
   "**" {:context star-ctx :validate-fn (constantly true)}
   "shape" {:context top-level-ctx :validate-fn (fn [v] (contains? shapes (dekey v)))}
   "label" {:context top-level-ctx :validate-fn (fn [v] (kstr? v))}
   "source-arrowhead" {:validate-fn (constantly true)}
   "target-arrowhead" {:validate-fn (constantly true)}
   "style" {:context top-level-ctx
            :validate-fn (fn [v]
                           (let [v (dekey v)]
                             (contains? style-attrs v)))}
   "near" {:context top-level-ctx
           :validate-fn (fn [v] (let [v (dekey v)]
                                  (contains?
                                   #{"top-left" "top-center" "top-right"
                                     "center-left" "center-right"
                                     "bottom-left" "bottom-center" "bottom-right"}
                                   v)))}
   "icon" {:context top-level-ctx :validate-fn (fn [v] (string? v))}
   "width" {:context top-level-ctx :validate-fn (fn [v] (integer? (tpp v)))}
   "height" {:context top-level-ctx :validate-fn (fn [v] (integer? (tpp v)))}
   "constraint" {:context top-level-ctx :validate-fn (fn [v] (string? v))}
   "direction" {:context top-level-ctx :validate-fn (fn [v] (let [v (dekey v)]
                                                              (contains?
                                                               #{"up" "down" "left" "right"}
                                                               v)))}
   "opacity" {:context style-ctx :style? true
              :validate-fn (fn [v] (let [v (tpp v)]
                                     (and (or (float? v) (integer? v))
                                          (>= v 0)
                                          (<= v 1))))}
   "fill" {:context style-ctx :style? true :validate-fn (fn [v] (or (nil? v) (valid-color? v)))}
   "filled" {:context [#{"source-arrowhead" "target-arrowhead"} "style"]
             :validate-fn (fn [v] (boolean? (tpp v)))}
   "stroke" {:context style-ctx :style? true :validate-fn (fn [v] (or (nil? v) (valid-color? v)))}
   "stroke-width" {:context style-ctx :style? true :validate-fn (partial int-between? 0 15)}
   "stroke-dash" {:context style-ctx :style? true :validate-fn (partial int-between? 1 10)}
   "border-radius" {:context style-ctx :style? true :validate-fn (partial int-between? 0 20)}
   "font-color" {:context style-ctx :style? true :validate-fn (fn [v] (or (nil? v) (valid-color? v)))}
   "shadow" {:context style-ctx :style? true :validate-fn (fn [v] (boolean? (tpp v)))}
   "multiple" {:context style-ctx :style? true :validate-fn (fn [v] (boolean? (tpp v)))}
   "3d" {:context style-ctx :style? true :validate-fn (fn [v] (boolean? (tpp v)))}
   "animated" {:context style-ctx :style? true :validate-fn (fn [v] (boolean? (tpp v)))}
   "link" {:context top-level-ctx :validate-fn (fn [v] (string? v))}
   "font-size" {:context style-ctx :style? true :validate-fn (partial int-between? 8 100)}
   "tooltip" {:validate-fn (fn [v] (string? v))}
   "italic" {:context style-ctx :style? true :validate-fn (fn [v] (boolean? (tpp v)))}
   "bold" {:context style-ctx :style? true :validate-fn (fn [v] (boolean? (tpp v)))}
   "double-border" {:context style-ctx :style? true :validate-fn (fn [v] (boolean? (tpp v)))}
   "underline" {:context style-ctx :style? true :validate-fn (fn [v] (boolean? (tpp v)))}
   "font" {:context style-ctx :style? true :validate-fn (fn [v] (= "mono" (str v)))}
   "fill-pattern" {:context style-ctx :style? true
                   :validate-fn (fn [v] (let [v (dekey v)]
                                          (contains?
                                           #{"dots" "lines" "grain" "none"} v)))}
   "class" {:context top-level-ctx :validate-fn (fn [v] (kstr? v))}
   "grid-rows" {:context top-level-ctx :validate-fn (fn [v] (integer? (tpp v)))}
   "grid-columns" {:context top-level-ctx :validate-fn (fn [v] (integer? (tpp v)))}
   "vertical-gap" {:context top-level-ctx :validate-fn (fn [v] (integer? (tpp v)))}
   "horizontal-gap" {:context top-level-ctx :validate-fn (fn [v] (integer? (tpp v)))}
   "grid-gap" {:context top-level-ctx :validate-fn (fn [v] (integer? (tpp v)))}
   "text-transform" {:context style-ctx :style? true
                     :validate-fn
                     (fn [v]
                       (let [v (dekey v)]
                         (contains? #{"uppercase" "lowercase" "title" "none"} v)))}})


;; ********* Public api *************


;; ********* Context *************
;; context is whether an attribute occurs in the right position, e.g.
;; is `fill` under a `style` key.


(defn- matches-context? [context ref-context]
  (let [[c1 & crest] context
        [r1 & rrest] ref-context]
    (cond
      (set? r1)  (if (contains? r1 c1)
                   (matches-context? crest rrest)
                   false)

      (vector? r1) (if (and (= :last (first r1))
                            (= (second r1) (last context)))
                     true
                     false)

      :else (= r1 c1))))


(defn- ref-ctx [k] (-> (get d2-attributes-map k) :context))


(defn- str-context [ctx]
  (apply str
         (interpose " > "
                    (map
                     (fn [ci]
                       (if (set? ci)
                         (apply str
                                (interpose "/"
                                           (map #(str "'" % "'") ci)))
                         (str "'" ci "'")))
                     ctx))))


(defn- context-error [k context ref-context elem]
  (cond
    (and context
         ref-context)    (err (str
                               "'" k "' can only come after: "
                               (str-context ref-context) ", in " elem))

    context              (err (str
                               "'" k "' cannot come after '"
                               (last context) "', in " elem))

    ref-context          (err (str
                               "'" k "' must come after "
                               (str-context ref-context) ", in " elem))))


(defn in-context?
  "Checks whether the provided context of the d2-keyword k is in the right context.
   Throws an error if not."
  [k ctx elem]
  (let [ref-ctx (ref-ctx k)]
    (if (matches-context? ctx ref-ctx)
      true
      (context-error k ctx ref-ctx elem))))


;; ********* Validation *************

(defn- validation-error [k v elem]
  (err (str "attr: '" k " " v "' failed validation, in " elem)))


(defn- validation-fn [k] (-> (get d2-attributes-map k) :validate-fn))


(defn validate-attr [elem k v]
  (let [f (validation-fn k)]
    (try
      (if (f v)
        true
        (validation-error k v elem))
      (catch Exception _ (validation-error k v elem)))))


(defn validate-attrs [elem k vs]
  (every?
   (fn [v] (validate-attr elem k v))
   vs))


;; ********* Attrs *************

(def ^:private d2-attributes (into #{} (keys d2-attributes-map)))


(defn key?
  "Is k a d2 key"
  [k]
  (contains? d2-attributes (name k)))


;; ****(used in parsing)******

(def ^:private style-attrs
  (into #{} (cons "filled"
                  (keys
                   (filter
                    (fn [[_ v]] (= (:context v) [[:last "style"]]))
                    d2-attributes-map)))))


(defn style-attr? [d2-keyword]
  (contains? style-attrs d2-keyword))


;; remove '*' and '**' as required for parsing conn-keys
(defn d2-keys []
  (apply str
         (interpose "|"
                    (map #(str "'" % "'")
                         (->> d2-attributes (remove #(or (= % "*") (= % "**"))))))))
