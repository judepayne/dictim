# Table of contents
-  [`dictim.d2.compile`](#dictim.d2.compile)  - Namespace for compiling dictim to d2.
    -  [`d2`](#dictim.d2.compile/d2) - Converts dictim elements to a formatted d2 string.
-  [`dictim.d2.parse`](#dictim.d2.parse)  - Namespace for parsing d2 and returning dictim format.
    -  [`dictim`](#dictim.d2.parse/dictim) - Converts a d2 string to its dictim representation.
    -  [`num-parses`](#dictim.d2.parse/num-parses)
    -  [`parses-d2`](#dictim.d2.parse/parses-d2)
-  [`dictim.dot.compile`](#dictim.dot.compile)  - Namespace for compiling dictim to graphviz' dot.
    -  [`dot`](#dictim.dot.compile/dot) - Converts dictim elements to a (formatted) dot string.
-  [`dictim.flat`](#dictim.flat)  - Namespace for flattening and (re)building dictim.
    -  [`build`](#dictim.flat/build) - Builds a sequence of flat-dictim elements into dictim.
    -  [`flat`](#dictim.flat/flat) - Flattens dictim elements into flat-dictim format.
-  [`dictim.format`](#dictim.format)  - Namespace for formatting d2 output of dictim.
    -  [`fmt`](#dictim.format/fmt) - Formats a d2 string.
-  [`dictim.json`](#dictim.json)  - Namespace for converting dictim to/ from json.
    -  [`from-json`](#dictim.json/from-json) - Desrializes the json to dictim.
    -  [`to-json`](#dictim.json/to-json) - Serializes the dictim to json.
-  [`dictim.template`](#dictim.template)  - Namespace for handling dictim templates.
    -  [`add-styles`](#dictim.template/add-styles) - Walks the supplied dictim edn and decorates with attrs and top level directives.
    -  [`children`](#dictim.template/children) - Returns the children elements of a dictim element.
    -  [`element-type`](#dictim.template/element-type) - Returns the dictim element type as a string.
    -  [`key`](#dictim.template/key) - Returns the key of a dictim element.
    -  [`keys`](#dictim.template/keys) - returns the keys of a dictim element.
    -  [`label`](#dictim.template/label) - Returns the label of a dictim element.
    -  [`postwalk-dictim`](#dictim.template/postwalk-dictim) - Similar to clojure.walk/postwalk but for dictim.
    -  [`prewalk-dictim`](#dictim.template/prewalk-dictim) - Similar to clojure.walk/prewalk but for dictim.
    -  [`remove-styles`](#dictim.template/remove-styles) - removes all maps from the nested form.
    -  [`walk-dictim`](#dictim.template/walk-dictim) - Similar to clojure.walk/walk but refined for dictim.

-----
# <a name="dictim.d2.compile">dictim.d2.compile</a>


Namespace for compiling dictim to d2




## <a name="dictim.d2.compile/d2">`d2`</a><a name="dictim.d2.compile/d2"></a>
``` clojure

(d2 & elems)
```

Converts dictim elements to a formatted d2 string.
   Validates each element, throws an error if invalid.
<p><sub><a href="https://github.com/judepayne/dictim/blob/main/src/dictim/d2/compile.cljc#L166-L175">Source</a></sub></p>

-----
# <a name="dictim.d2.parse">dictim.d2.parse</a>


Namespace for parsing d2 and returning dictim format.




## <a name="dictim.d2.parse/dictim">`dictim`</a><a name="dictim.d2.parse/dictim"></a>
``` clojure

(dictim
 d2
 &
 {:keys [key-fn label-fn flatten-lists? retain-empty-lines?],
  :or {key-fn identity, label-fn str/trim, flatten-lists? false, retain-empty-lines? false}})
```

Converts a d2 string to its dictim representation.
   Each dictim element returned's type is captured in the :tag key
   of the element's metadata.
   Three optional functions may be supplied:
     :key-fn             a modifier applied to each key.
     :label-fn           a modifier applied to each label.
     :flatten-lists?     if true, flattens lists where every element is a shape
                         with just a key, & no label or attrs.
     :retain-empty-lines?  If true, empty lines are retained in the dictim output.
<p><sub><a href="https://github.com/judepayne/dictim/blob/main/src/dictim/d2/parse.cljc#L252-L370">Source</a></sub></p>

## <a name="dictim.d2.parse/num-parses">`num-parses`</a><a name="dictim.d2.parse/num-parses"></a>
``` clojure

(num-parses d2 & kvs)
```
<p><sub><a href="https://github.com/judepayne/dictim/blob/main/src/dictim/d2/parse.cljc#L242-L243">Source</a></sub></p>

## <a name="dictim.d2.parse/parses-d2">`parses-d2`</a><a name="dictim.d2.parse/parses-d2"></a>
``` clojure

(parses-d2 d2 & kvs)
```
<p><sub><a href="https://github.com/judepayne/dictim/blob/main/src/dictim/d2/parse.cljc#L236-L238">Source</a></sub></p>

-----
# <a name="dictim.dot.compile">dictim.dot.compile</a>


Namespace for compiling dictim to graphviz' dot




## <a name="dictim.dot.compile/dot">`dot`</a><a name="dictim.dot.compile/dot"></a>
``` clojure

(dot & elems)
```

Converts dictim elements to a (formatted) dot string.
   Validates each element, throws an error if invalid.
<p><sub><a href="https://github.com/judepayne/dictim/blob/main/src/dictim/dot/compile.cljc#L302-L317">Source</a></sub></p>

-----
# <a name="dictim.flat">dictim.flat</a>


Namespace for flattening and (re)building dictim.




## <a name="dictim.flat/build">`build`</a><a name="dictim.flat/build"></a>
``` clojure

(build flat-elems)
```

Builds a sequence of flat-dictim elements into dictim.
<p><sub><a href="https://github.com/judepayne/dictim/blob/main/src/dictim/flat.cljc#L152-L163">Source</a></sub></p>

## <a name="dictim.flat/flat">`flat`</a><a name="dictim.flat/flat"></a>
``` clojure

(flat elems)
```

Flattens dictim elements into flat-dictim format.
<p><sub><a href="https://github.com/judepayne/dictim/blob/main/src/dictim/flat.cljc#L122-L131">Source</a></sub></p>

-----
# <a name="dictim.format">dictim.format</a>


Namespace for formatting d2 output of dictim.




## <a name="dictim.format/fmt">`fmt`</a><a name="dictim.format/fmt"></a>
``` clojure

(fmt d2s & {:keys [tab], :or {tab 2}})
```

Formats a d2 string. :tab is the width of one indentation step.
<p><sub><a href="https://github.com/judepayne/dictim/blob/main/src/dictim/format.cljc#L45-L66">Source</a></sub></p>

-----
# <a name="dictim.json">dictim.json</a>


Namespace for converting dictim to/ from json.




## <a name="dictim.json/from-json">`from-json`</a><a name="dictim.json/from-json"></a>
``` clojure

(from-json js & args)
```

Desrializes the json to dictim
<p><sub><a href="https://github.com/judepayne/dictim/blob/main/src/dictim/json.cljc#L20-L27">Source</a></sub></p>

## <a name="dictim.json/to-json">`to-json`</a><a name="dictim.json/to-json"></a>
``` clojure

(to-json dictim & {:as opts})
```

Serializes the dictim to json.
<p><sub><a href="https://github.com/judepayne/dictim/blob/main/src/dictim/json.cljc#L9-L12">Source</a></sub></p>

-----
# <a name="dictim.template">dictim.template</a>


Namespace for handling dictim templates




## <a name="dictim.template/add-styles">`add-styles`</a><a name="dictim.template/add-styles"></a>
``` clojure

(add-styles dict template)
(add-styles dict template directives)
```

Walks the supplied dictim edn and decorates with attrs and top level directives.
   template can be either:
     - a function that takes an elem and returns the attrs to be added
       to the elem (or nil).
     - a sequence of test and attribute pairs to be applied if the test succeeds.
       A simple test has the form [<comparator> <accessor> <value>] where:
       - <comparator> is a string, one of the [`comparators`](#dictim.template/comparators) var's keys in
         this namespace.
       - <accessor> is a string, one of the [`accessors`](#dictim.template/accessors) var's keys in this
         namespace. Accessors allow you to access values of a dictim element,
         for example its [`key`](#dictim.template/key) or [`label`](#dictim.template/label).
       - <value> is the value to be tested against.
         Example simple test: `["=" "key" "node123"]`
       A nested test nests simple tests with `and` and `or` statements.
         Example nested test:
         `["and" ["=" "type" "ctr"] ["=" "key" "node123"]]`
       Attributes are supplied using standard dictim, e.g. `{:style.fill "red"}`
   directives is a map of attrs to be added at the top level e.g. `{"classes"...}`
   If there are directives in the original dict, the new directives will be merge over them.
<p><sub><a href="https://github.com/judepayne/dictim/blob/main/src/dictim/template.cljc#L260-L301">Source</a></sub></p>

## <a name="dictim.template/children">`children`</a><a name="dictim.template/children"></a>




Returns the children elements of a dictim element
<p><sub><a href="https://github.com/judepayne/dictim/blob/main/src/dictim/template.cljc#L46-L46">Source</a></sub></p>

## <a name="dictim.template/element-type">`element-type`</a><a name="dictim.template/element-type"></a>
``` clojure

(element-type elem)
```

Returns the dictim element type as a string. Either:
   shape, conn, conn-ref, ctr, attrs, quikshape, cmt, empty-lines, list, elements or nil
<p><sub><a href="https://github.com/judepayne/dictim/blob/main/src/dictim/template.cljc#L69-L73">Source</a></sub></p>

## <a name="dictim.template/key">`key`</a><a name="dictim.template/key"></a>




Returns the key of a dictim element
<p><sub><a href="https://github.com/judepayne/dictim/blob/main/src/dictim/template.cljc#L19-L19">Source</a></sub></p>

## <a name="dictim.template/keys">`keys`</a><a name="dictim.template/keys"></a>




returns the keys of a dictim element
<p><sub><a href="https://github.com/judepayne/dictim/blob/main/src/dictim/template.cljc#L56-L56">Source</a></sub></p>

## <a name="dictim.template/label">`label`</a><a name="dictim.template/label"></a>




Returns the label of a dictim element
<p><sub><a href="https://github.com/judepayne/dictim/blob/main/src/dictim/template.cljc#L28-L28">Source</a></sub></p>

## <a name="dictim.template/postwalk-dictim">`postwalk-dictim`</a><a name="dictim.template/postwalk-dictim"></a>
``` clojure

(postwalk-dictim f element)
```

Similar to clojure.walk/postwalk but for dictim.
<p><sub><a href="https://github.com/judepayne/dictim/blob/main/src/dictim/template.cljc#L242-L245">Source</a></sub></p>

## <a name="dictim.template/prewalk-dictim">`prewalk-dictim`</a><a name="dictim.template/prewalk-dictim"></a>
``` clojure

(prewalk-dictim f element)
```

Similar to clojure.walk/prewalk but for dictim.
<p><sub><a href="https://github.com/judepayne/dictim/blob/main/src/dictim/template.cljc#L248-L251">Source</a></sub></p>

## <a name="dictim.template/remove-styles">`remove-styles`</a><a name="dictim.template/remove-styles"></a>
``` clojure

(remove-styles dict & {:keys [retain-vars?], :or {retain-vars? false}})
```

removes all maps from the nested form. i.e. attrs and directives.
   If retain-vars? is true, :vars/"vars" attributes will be retained in the dictim,
   since vars can be part of the 'data side' of a piece of dictim.
<p><sub><a href="https://github.com/judepayne/dictim/blob/main/src/dictim/template.cljc#L304-L322">Source</a></sub></p>

## <a name="dictim.template/walk-dictim">`walk-dictim`</a><a name="dictim.template/walk-dictim"></a>
``` clojure

(walk-dictim inner outer element)
```

Similar to clojure.walk/walk but refined for dictim. Only walks into
   children of elements.
<p><sub><a href="https://github.com/judepayne/dictim/blob/main/src/dictim/template.cljc#L230-L239">Source</a></sub></p>
