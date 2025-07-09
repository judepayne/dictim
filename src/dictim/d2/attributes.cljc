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


(def ^:private shapes #{"rectangle" "square" "page" "parallelogram" "document" "cylinder" "queue" "package" "step" "callout" "stored_data" "person" "diamond" "oval" "circle" "hexagon" "cloud" "sql_table" "class" "sequence_diagram" "text" "c4-person"})


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

(def ^:private star-ctx [#{[] nil "source-arrowhead" "target-arrowhead"}])
(def ^:private top-level-ctx [#{nil "source-arrowhead" "target-arrowhead" "*" "**" "src" "dst"}])
(def ^:private style-ctx [[:last "style"]])


(declare ^:private style-attrs)


(def d2-attributes-db
  {"*" {:context star-ctx :validate-fn (constantly true)}
   "**" {:context star-ctx :validate-fn (constantly true)}
   "shape" {:context top-level-ctx
            :prefixes #{"&" "!&"}
            :validate-fn (fn [v] (contains? shapes (dekey v)))
            :help "Expected: rectangle, square, circle, diamond, etc."}
   "label" {:context top-level-ctx :validate-fn (fn [v] (kstr? v))}
   "source-arrowhead" {:validate-fn (constantly true)}
   "target-arrowhead" {:validate-fn (constantly true)}
   "style" {:context top-level-ctx
            :validate-fn (fn [v]
                           (let [v (dekey v)]
                             (contains? style-attrs v)))
            :prefixes #{"&" "!&"}}
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
                                                               v)))
                :help "Expected: up, down, left, or right"}
   "opacity" {:context style-ctx :style? true
              :validate-fn (fn [v] (let [v (tpp v)]
                                     (and (or (float? v) (integer? v))
                                          (>= v 0)
                                          (<= v 1))))
              :help "Expected: number between 0-1"}
   "fill" {:context style-ctx :style? true :validate-fn (fn [v] (or (nil? v) (valid-color? v)))
           :help "Expected: hex color (#fff), CSS color name, or gradient"}
   "filled" {:context [#{"source-arrowhead" "target-arrowhead"} "style"]
             :validate-fn (fn [v] (boolean? (tpp v)))}
   "stroke" {:context style-ctx :style? true :validate-fn (fn [v] (or (nil? v) (valid-color? v)))
             :help "Expected: hex color (#fff), CSS color name, or gradient"}
   "stroke-width" {:context style-ctx :style? true :validate-fn (partial int-between? 0 15)
                   :help "Expected: integer between 0-15"}
   "stroke-dash" {:context style-ctx :style? true :validate-fn (partial int-between? 1 10)
                  :help "Expected: integer between 1-10"}
   "border-radius" {:context style-ctx :style? true :validate-fn (partial int-between? 0 20)
                     :help "Expected: integer between 0-20"}
   "font-color" {:context style-ctx :style? true :validate-fn (fn [v] (or (nil? v) (valid-color? v)))
                 :help "Expected: hex color (#fff), CSS color name, or gradient"}
   "shadow" {:context style-ctx :style? true :validate-fn (fn [v] (boolean? (tpp v)))}
   "multiple" {:context style-ctx :style? true :validate-fn (fn [v] (boolean? (tpp v)))}
   "3d" {:context style-ctx :style? true :validate-fn (fn [v] (boolean? (tpp v)))}
   "animated" {:context style-ctx :style? true :validate-fn (fn [v] (boolean? (tpp v)))}
   "link" {:context top-level-ctx
           :prefixes #{"&" "!&"}
           :validate-fn (fn [v] (string? v))}
   "font-size" {:context style-ctx :style? true :validate-fn (partial int-between? 8 100)
                :help "Expected: integer between 8-100"}
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
                         (contains? #{"uppercase" "lowercase" "title" "none"} v)))}
   "level" {:context top-level-ctx
            :prefixes #{"&"}
            :must-be-prefixed? true
            :validate-fn (fn [v] (integer? (tpp v)))}
   "src" {:context top-level-ctx
          :prefixes #{"&" "!&"}
          :must-be-prefixed? true
          :validate-fn (constantly true)}
   "dst" {:context top-level-ctx
          :prefixes #{"&" "!&"}
          :must-be-prefixed? true
          :validate-fn (constantly true)}})


;; D2Key record and protocol for cleaner prefix handling
(defrecord D2Key [prefix value]
  Object
  (toString [this]
    (if prefix
      (str prefix value)
      (str value))))

(defprotocol D2Keyable
  "Protocol for working with D2 keys that may have prefixes"
  (d2-prefix [this] "Returns the prefix string, or nil if no prefix")
  (d2-value [this] "Returns the unprefixed key part") 
  (d2-prefixed? [this] "Returns true if the key has a prefix")
  (d2-valid-prefix? [this] "Returns true if the prefix is valid for this key")
  (d2-must-be-prefixed? [this] "Returns true if this key must have a prefix"))

;; Original split-prefix function - keep for internal use
(defn- split-prefix
  "Separates leading '!&' or '&' prefix from a string.
     Returns [prefix remainder] if prefix exists, otherwise returns the string unchanged."
  [s]
  (if-let [match (re-find #"^(!&|&)(.*)$" s)]
    [(nth match 1) (nth match 2)]
    s))

;; Constructor for D2Key
(defn ->d2-key [s]
  "Creates a D2Key from a string, parsing any prefix"
  (if (instance? D2Key s)
    s
    (let [result (split-prefix s)]
      (if (vector? result)
        (->D2Key (first result) (second result))
        (->D2Key nil result)))))

;; Protocol implementation
(extend-protocol D2Keyable
  D2Key
  (d2-prefix [this] (:prefix this))
  (d2-value [this] (:value this))
  (d2-prefixed? [this] (some? (:prefix this)))
  (d2-valid-prefix? [this] 
    (when (d2-prefixed? this)
      (if-let [permitted-prefixes (:prefixes (get d2-attributes-db (d2-value this)))]
        (contains? permitted-prefixes (d2-prefix this))
        false)))
  (d2-must-be-prefixed? [this]
    (:must-be-prefixed? (get d2-attributes-db (d2-value this)))))

;; utility functions for handling prefixed keys - now using D2Key internally

;; ********* Context *************
;; context is whether an attribute occurs in the right position, e.g.
;; is `fill` under a `style` key.


(defn- matches-context? [context ref-context]
  (let [[c1 & crest] context
        [r1 & rrest] ref-context]
    (cond
      (set? r1)  (let [c1-d2key (when c1 (->d2-key c1))
                        c1-unprefixed (if (and c1-d2key (d2-prefixed? c1-d2key)) 
                                        (d2-value c1-d2key) 
                                        c1)]
                   (if (contains? r1 c1-unprefixed)
                     (matches-context? crest rrest)
                     false))

      #_(and (vector? context)
             (not (vector? ref-context)))  #_false
      
      (vector? r1) (if (and (= :last (first r1))
                            (= (second r1) (last context)))
                     true
                     false)

      :else (= r1 c1))))


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


(defn- ref-ctx 
  "Returns the reference context for a d2 key, handling prefixes internally"
  [k]
  (let [d2key (->d2-key k)]
    (if (d2-prefixed? d2key)
      (when (d2-valid-prefix? d2key)
        (-> (get d2-attributes-db (d2-value d2key)) :context))
      (-> (get d2-attributes-db (d2-value d2key)) :context))))

;; Public API

(defn in-context?
  "Checks whether the provided context of the d2-keyword k is in the right context.
     Throws an error if not."
  [k ctx elem]
  (let [d2key (->d2-key k)
        ref-ctx (ref-ctx k)]
    (cond
      ;; For prefixed keys, validate using the unprefixed context
      (d2-prefixed? d2key)
      (if (matches-context? ctx ref-ctx)
        true
        (context-error k ctx ref-ctx elem))

      ;; For non-prefixed keys that must be prefixed, error
      (d2-must-be-prefixed? d2key)
      (err (str k " must be properly prefixed."))

      ;; For normal non-prefixed keys, validate context
      :else
      (if (matches-context? ctx ref-ctx)
        true
        (context-error k ctx ref-ctx elem)))))


;; ********* Validation *************

;; *** Validation contexts **
;; validation context are 'overrides' to the normal per attribute
;; validation-fn, to be used when the context is different
(defn nested-strings?
    "Returns true if x is:
     (i) a string (ii) vector/nested vector of strings, recursively"
    [x]
    (cond
      (string? x)
      true

      (keyword? x)
      true

      (vector? x)
      (every? nested-strings? x)

      :else
      false))


(def validation-context-db
  {:template ;; the template context
   ;; templates have more powerful labels than normal d2 attrs. They can do string interpolation
   {"label" {:validate-fn (fn [v] (nested-strings? v))}}})


(defn- validation-error [k v elem]
  (let [d2key (->d2-key k)
        attr-name (d2-value d2key)
        attr-info (get d2-attributes-db attr-name)
        help-text (or (:help attr-info) "Expected: valid value")]
    (if attr-info
      ;; Known attribute, invalid value
      (err (str attr-name " validation failed: '" v "' is invalid. " 
                help-text ". In: " elem))
      ;; Unknown attribute  
      (if (str/includes? k ".")
        (let [parts (str/split k #"\." 2)
              prefix (first parts)
              suffix (second parts)]
          (err (str "'" suffix "' is not a valid sub-attr of " prefix ", in: " elem)))
        (err (str "Unknown attribute: '" k "' in " elem))))))


(defn- validation-fn [k ctx]
  (let [d2key (d2-value (->d2-key k))]
    (if-let [overrides (get (validation-context-db ctx) d2key)]
      (or (:validate-fn overrides)
          (-> (get d2-attributes-db d2key) :validate-fn))
      
      (-> (get d2-attributes-db d2key) :validate-fn))))


(defn validate-attr [elem k v ctx]
  (if-let [f (validation-fn k ctx)]
    (try
      (if (f v)
        true
        (validation-error k v elem))
      (catch Exception _ (validation-error k v elem)))
    ;; No validation function = unknown attribute
    (validation-error k v elem)))


(defn validate-attrs [elem k vs ctx]
  (if (= "style" k)
    ;; For style, validate that each key is a valid style attribute
    (every?
     (fn [style-key]
       (let [style-key-str (if (keyword? style-key) (name style-key) (str style-key))]
         (if (contains? style-attrs style-key-str)
           true
           (validation-error (str k "." style-key-str) nil elem))))
     vs)
    ;; For other attributes, use the original logic
    (every?
     (fn [v] (validate-attr elem k v ctx))
     vs)))


;; ********* Attrs *************

(def ^:private d2-attributes (into #{} (keys d2-attributes-db)))


(defn key?
  "Is k a d2 key"
  [k]
  (let [d2key (->d2-key k)]
    (contains? d2-attributes (name (d2-value d2key)))))


;; ****(used in parsing)******

(def ^:private style-attrs
  (into #{} (cons "filled"
                  (keys
                   (filter
                    (fn [[_ v]] (= (:context v) [[:last "style"]]))
                    d2-attributes-db)))))


(defn style-attr? [d2-keyword]
  (contains? style-attrs d2-keyword))


;; remove '*' and '**' as required for parsing conn-keys
(defn d2-keys []
  (apply str
         (interpose "|"
                    (map #(str "'" % "'")
                         (->> d2-attributes (remove #(or (= % "*") (= % "**"))))))))
