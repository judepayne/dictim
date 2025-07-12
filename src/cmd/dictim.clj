;; babashka script for the cmd line tool
(ns cmd.dictim
  (:require [dictim.d2.compile :as c]
            [dictim.d2.parse :as p]
            [dictim.json :as json]
            [dictim.template :as tmp]
            [dictim.flat :as flat]
            [dictim.walk :as walk]
            [dictim.validate :as v]
            [hiccup.core :refer [html]]
            [babashka.cli :as cli]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [cmd.serve :as serve]
            [cmd.file-watcher :as fw]
            [babashka.fs :as fs]
            [dictim.graphspec :as g]
            [babashka.process :refer [shell]])
  (:refer-clojure :exclude [compile])
  (:gen-class))


(defn- exception [msg]
  (throw (ex-info msg {})))   ;; (Exception. msg) is stripped away by GraalVM. ex-info fine


(defn- f [] (println "I do nothing"))


(defn show-help
  [spec]
  (cli/format-opts (merge spec {:order [:compile :cw :image :d :layout :theme :scale :sketch :pad :center :dark-theme :animate-interval :iw :parse :keywordize :j :m :pw :stringify :apply-tmp :aw :template :output :graph :gw :giw :flatten :build :validate :r :version :help]})))


(def compile-help
  "Compiles supplied dictim to d2
The value supplied to --compile may be either
  - a edn/ json dictim syntax string (in single quotes)
  - ommitted in which case *std-in* is read

An edn/ json template file may be specified via
--template/ -t which causes the template to be applied
during compilation.

compile may be used with watch (--watch/ -w) in which
case, the watched file will be recompiled whenever it
changes. If a --template/-t file is also specified then
that file will also be watched. When used with watch, an
output file must be specified via the --output/ -o flag.")


(def parse-help
  "Parses supplied d2 into dictim
The value supplied to --parse may be either
  - a d2 string (in single quotes)
  - ommitted in which case *std-in* is read

parse may be used with watch (--watch/ -w) in which
case, the watched file will be recompiled whenever it
changes. When used with watch, an output file must be
specified via the --output/ -o flag.
--parse has various other supplemental flags: -k, -j & -m\n\n")


(def apply-template-help
  "Applies a dictim template to d2.
Parses suppled d2 into dictim (taking the usual optional
supplemental flags for parse), merges in a dictim template
specified via the --template/ -t otpion, and compiles the
result back into d2.

apply-tmp may be used with watch (--watch/ -w) in which
case, the watched file will be round-tripped whenever it
changes. The specified template file will also be
watched. When used with watch, an output file must be
specified via the --output/ -o flag.")


(def image-help
  "Compiles dictim to d2 and renders to SVG diagram.
The value supplied to --image may be either
  - a edn/ json dictim syntax string (in single quotes)
  - omitted in which case *std-in* is read
Requires d2 to be installed and available on your path.
Supports the same d2 options as watch mode: layout, theme, scale.
Use with -o to specify output file, otherwise outputs to stdout.

image may be used with watch (--watch/ -w) in which
case, the watched file will be recompiled whenever it changes.
When used with watch:
- With --output/-o: continuously updates the SVG file
- Without --output: serves diagram in browser with live reload
--image has various sub options, most of which are passed through
to d2:")


(def graph-help
  "Converts a dictim graph spec to dictim.
applies a dictim template specified via the
--template/ -t option.
-j and -m options are also available.

graph may be used with watch (--watch/ -w) in which
case, the watched file will be recompiled whenever it changes.

Can also be used with (--image/ -i) in which case, an
image is either served (no --output specified) or written to disk.")


(def flatten-help
  "Flattens supplied dictim to dictim.flat syntax.
can be used with the -j and -m options.")


(def build-help
  "Builds supplied dictim.flat syntax into dictim syntax.
can be used with the -j and -m options.")


(def cli-spec
  {:spec
   {:compile {:desc compile-help
              :alias :c}
    :cw {:desc "Shorthand for -c -w (compile and watch)\n​"}
    :image {:desc image-help
            :alias :i}
    :d {:coerce :boolean
        :desc "    (dict) debug for Watch: Shows interim d2 in the browser."}
    :layout {:desc "    (d2) d2 layout engine name; dagre/ elk/ tala."
             :alias :l}
    :theme {:desc "    (d2) d2 theme id. See https://d2lang.com/tour/themes."
            :alias :th}
    :scale {:desc "    (d2) set the svg scaling factor used by d2. default is 1.0"
            :alias :s}
    :sketch {:coerce :boolean
           :desc "    (d2) Render diagram to look hand-sketched."}
    :pad {:desc "    (d2) Pixels padded around the rendered diagram."
          :coerce :int}
    :center {:coerce :boolean
             :desc "    (d2) Center the SVG in the containing viewbox."}
    :dark-theme {:desc "    (d2) Theme to use in dark mode browsers."
                 :coerce :int}
    :animate-interval {:desc "    (d2) Animate through boards at interval (milliseconds)."
                       :coerce :int} 
    :iw {:desc "Shorthand for -i -w (image and watch)\n​"}
    :parse {:desc parse-help
            :alias :p}
    :keywordize {:alias :k
                 :desc "    Converts edn format dictim keys to keywords."}
    :j {:coerce :boolean
        :desc "    Converts the output of parse to dictim syntax json."}
    :m {:coerce :boolean
        :desc "    Additional to  -j: prettifies the json output of parse."}
    :pw {:desc "Shorthand for -p -w (parse and watch)\n​"}
    :stringify {:alias :st
                :desc "Converts edn format dictim keys to strings.\n​"}
    :apply-tmp {:desc apply-template-help
                :alias :a}
    :aw {:desc "Shorthand for -a -w (apply template and watch)\n​"}
    :template {:desc "Path to an edn/ json template file."
               :alias :t}
    :output {:desc "Specifies the file to output to in watch mode.\n​"
             :alias :o}
    :graph {:desc graph-help
            :alias :g}
    :gw {:desc "Shorthand for -g -w (graph spec and watch)\n"}
    :giw {:desc "shorthand for -g -i -w (graph spec, watch and generate image\n​"}
    :flatten {:desc flatten-help
              :alias :f}
    :build {:desc build-help
            :alias :b}
    :validate {:desc "determines if supplied edn format dictim is valid."
               :alias :val}
    :r {:coerce :boolean
        :desc "Removes styles (attributes) from parsed d2, including any vars."}
    :version {:coerce :boolean
              :desc "Returns the version of this command line tool."
              :alias :v}
    :help {:coerce :boolean
           :desc "Displays this help.\nhttps://github.com/judepayne/dictim for more."
           :alias :h}}
   :error-fn
   (fn [{:keys [spec type cause msg option]}]
     (when (= :org.babashka/cli type)
       (case cause
         :require
         (println
          (format "Missing required argument: %s\n" option))
         :validate
         (println
          (format "%s does not exist!\n" msg)))))})


(defn- from-json [maybe-json]
  (try
    (when-let [dict (json/from-json maybe-json)]
      dict)
    (catch java.io.IOException ex (do (.getName (class ex)) nil))))


(defn- from-edn [maybe-edn]
  (try
    (let [reader (java.io.PushbackReader. (java.io.StringReader. maybe-edn))
          forms (loop [forms []]
                  (let [form (try (edn/read reader) (catch Exception _ ::eof))]
                    (if (= form ::eof)
                      forms
                      (recur (conj forms form)))))]
      (cond
        (empty? forms) nil
        (= 1 (count forms)) (first forms)
        :else forms))
    (catch Exception ex (do (.getName (class ex)) nil))))


(defn- looks-like-filename? [s]
  "Heuristic to detect if a string looks like a filename rather than data"
  (and (string? s)
       (not (str/blank? s))
       (or
        ;; Contains common file extensions
        (re-find #"\.(edn|json|d2|clj|txt)$" s)
        ;; Contains path separators  
        (re-find #"[/\\]" s)
        ;; Looks like a relative path
        (re-find #"^\.{1,2}/" s)
        ;; Exists as a file (check safely)
        (try
          (and (fs/exists? s) (fs/regular-file? s))
          (catch Exception _ false)))))


;; removed as much causing stdin to be read in a non blocking fashion, which is
;; fine for files, but not useful behaviour when waiting for data from an api (for ex.)
#_(defn- stdin-has-data? []
    "Check if stdin has data available without blocking"
    (try
      (let [available (.available System/in)]
        (> available 0))
      (catch Exception _ false)))


(defn- handle-in [arg]
  (cond
    (looks-like-filename? arg)
    (exception (str "It looks like you're trying to pass a filename '" arg "'.\n"
                    "Use stdin redirection instead: Put '<' in front of the filename."))

    (true? arg)   (slurp *in*)


    :else arg))


(defn- read-data [data]
  (if-let [dict (from-json data)]
    [:json dict]
    (if-let [dict (from-edn data)]
      [:edn dict]
      (exception "invalid edn/ json!"))))


(defn- compile-fn [dict]
  (cond
    (symbol? dict)
    (exception (str "'" dict "' isn't valid dictim syntax. Did you mean '..< " dict  "'?"))
    
    (empty? dict)
    (exception "no dictim to compile")
    
    (and (sequential? dict) (every? coll? dict))
    (apply c/d2 dict)

    dict
    (c/d2 dict)

    :else
    (exception "Could not read input as valid dictim syntax")))


(defn- apply-dictim-template [dict template]
  (try
    (tmp/apply-template dict template)
    (catch clojure.lang.ExceptionInfo ex
      (exception (.getMessage ex)))
    (catch Exception ex
      (exception "template file not found"))))


(defn- compile-impl [opts in]
  (let [[_ dict] (read-data in)
        template-file (or (:template opts) (:m opts))
        template (when template-file (second (read-data (slurp template-file))))]
    (if template
      (-> (apply-dictim-template dict template) compile-fn println)
      (-> dict compile-fn println))))


(defn- compile [opts]
  (compile-impl opts (handle-in (or (:compile opts) (:c opts)))))


(defn- compile-watch [opts]
  (when-not (or (:output opts) (:o opts))
    (exception "An --output/-o file should be specified."))
  (let [path (or (:watch opts) (:w opts))
        tmp-path (or (:template opts) (:t opts))
        out-path (or (:output opts) (:o opts))
        f (fn [] (with-open [out-data (io/writer out-path)]
                   (binding [*out* out-data]
                     (compile-impl opts (slurp path)))))]
    (f) ;; do once when first called
    (fw/add-watch path f)
    (when tmp-path (fw/add-watch tmp-path f))
    @(promise)))


(defn- format-error [s err]
  (apply str
         err "\n"
         (interleave
          (map
           (fn [idx s]
             (format "%3d: %s" idx s))
           (range)
           (str/split-lines s))
          (repeat "\n"))))


(defn installed? [the-command]
  (try
    (:out (shell {:out :string} "command -v" the-command))
    (catch Exception _ false)))


(def path-to-d2 "d2")


(def d2-default-theme 0)


(def d2-default-layout "dagre")


(def d2-default-scale 0.9)


(defn- d2-args [d2-opts]
  (let [m (into {} (filter #(-> % val some?) d2-opts))]
    (flatten
     (mapv
      (fn [[k v]]
        (if (boolean? v)
          (when v [(str "--" (name k))]) ;; stop passing --sketch true to d2
          [(str "--" (name k)) (str v)]))
      m))))


;; correct command line is:   echo "x -> y: hello" | d2 --layout tala -
(defn d2->svg
  "Takes a string of d2, and returns a string containing SVG."
  [d2 & {:keys [layout theme scale dark-theme sketch pad center animate-interval]
         :or {layout d2-default-layout
              theme d2-default-theme
              scale d2-default-scale}
         :as d2-opts}]
  (let [shell-args (flatten [{:out :string :in d2} "d2" (d2-args d2-opts) "-"])
        {:keys [out err]} (apply shell shell-args)]
    (or
     out
     (str "d2 engine error: "(format-error d2 err)))))

;; Extract common d2 option processing
(defn- extract-d2-opts [opts]
  (let [layout (or (:layout opts) (:l opts))
        theme (or (:theme opts) (:th opts))
        dark-theme (:dark-theme opts)
        sketch (:sketch opts)
        pad (:pad opts)
        center (:center opts)
        animate-interval (:animate-interval opts)
        scale (or (:scale opts) (:s opts))]
    (cond-> nil
      layout (assoc :layout layout)
      theme  (assoc :theme theme)
      scale  (assoc :scale scale)
      dark-theme (assoc :dark-theme dark-theme)
      sketch (assoc :sketch sketch)
      pad (assoc :pad pad)
      center (assoc :center center)
      animate-interval (assoc :animate-interval animate-interval))))


;; Generic transform base that handles the common pipeline
(defn transform-base
  "Base transformation: data → dictim → d2 → SVG → HTML

  Takes a prepare-dictim-fn that handles data-specific processing:
  (fn [data template d2-opts] -> [final-dictim final-d2-opts])"
  [prepare-dictim-fn debug? d2-opts file-contents template-contents]
  (try
    (let [[_ data] (read-data file-contents)
          [_ tmp] (when template-contents (read-data template-contents))
          [dict final-d2-opts] (prepare-dictim-fn data tmp d2-opts)
          d2 (compile-fn dict)]
      (html [:div (d2->svg d2 final-d2-opts)]
            [:div (when debug? (str "<b>d2:</b><br>" (str/replace d2 #"\n" "<br>")))]))
    (catch Exception ex
      (.getMessage ex))))


;; Specialized transform functions
(defn transform 
  ([debug? d2-opts file-contents]
   (transform debug? d2-opts file-contents nil))
  ([debug? d2-opts file-contents template-contents]
   (transform-base
     (fn [dict tmp d2-opts]
       (let [final-dict (if tmp (apply-dictim-template dict tmp) dict)
             final-d2-opts (if tmp (dissoc d2-opts :layout :theme) d2-opts)]
         [final-dict final-d2-opts]))
     debug? d2-opts file-contents template-contents)))

(defn graph-transform 
  ([debug? d2-opts file-contents]
   (graph-transform debug? d2-opts file-contents nil))
  ([debug? d2-opts file-contents template-contents]
   (transform-base
     (fn [grph tmp d2-opts]
       (let [final-grph (if tmp (merge grph tmp) grph)
             dict (g/graph-spec->dictim final-grph)]
         [dict d2-opts]))
     debug? d2-opts file-contents template-contents)))

;; Generic image watch that works with any transform function
(defn- generic-image-watch [transform-fn opts]
  (let [file (or (:watch opts) (:w opts))
        template (or (:template opts) (:t opts))
        debug? (or (:d opts) false)
        d2-opts (extract-d2-opts opts)]
    (cond
      (and template (fs/exists? template) (fs/exists? file) (installed? path-to-d2))
      (serve/start (partial transform-fn debug? d2-opts) file template)

      (and (fs/exists? file) (installed? path-to-d2))
      (serve/start (partial transform-fn debug? d2-opts) file)

      (fs/exists? file) (exception "d2 does not appear to be installed on your path.")

      :else (exception "File does not exist"))))


(defn- parse-print [opts dict]
  (cond-> dict
    (:j opts)            (-> (json/to-json {:pretty (:m opts)}) println)

    (not (:j opts))      pp/pprint))


(defn- parse-impl [opts in]
  (cond-> in
    (:keywordize opts)            (p/dictim :key-fn keyword)
    (not (:keywordize opts))      p/dictim

    (:r opts)            tmp/remove-attrs))


(defn- parse [opts]
  (->>
   (parse-impl opts (handle-in (or (:parse opts) (:p opts))))
   (parse-print opts)))


(defn- parse-watch [opts]
  (when-not (or (:output opts) (:o opts))
    (exception "An --output/-o file should be specified."))
  (let [path (or (:watch opts) (:w opts))
        out-path (or (:output opts) (:o opts))
        f (fn [] (with-open [out-data (io/writer out-path)]
                   (binding [*out* out-data]
                     (->>
                      (parse-impl opts (slurp path))
                      (parse-print opts)))))]
    (f) ;; do once when first called
    (fw/add-watch path f)
    @(promise)))


(defn- apply-template-impl [opts in]
  (let [dict (parse-impl #_(assoc opts :r true) opts in)
        template-file (or (:template opts) (:t opts))
        template (when template-file (second (read-data (slurp template-file))))]
    (if template
      (-> (apply-dictim-template dict
                                 (if (:r opts)
                                   template
                                   (assoc template :merge? true)))
          compile-fn println)
      (-> dict compile-fn println))))


(defn- apply-template [opts]
  (apply-template-impl opts (handle-in (or (:apply-tmp opts) (:a opts)))))


(defn- image-watch [opts]
    (generic-image-watch transform opts))


(defn- image-impl [opts in]
  (when-not (installed? path-to-d2)
    (exception "d2 does not appear to be installed on your path."))
  (let [[_ dict] (read-data in)
        template-file (or (:template opts) (:t opts))
        template (when template-file (second (read-data (slurp template-file))))
        dict (if template (apply-dictim-template dict template) dict)
        d2 (compile-fn dict)
        layout (or (:layout opts) (:l opts))
        theme (or (:theme opts) (:th opts))
        scale (or (:scale opts) (:s opts))
        dark-theme (:dark-theme opts)
        sketch (:sketch opts)
        pad (:pad opts)
        center (:center opts)
        animate-interval (:animate-interval opts)]
    (d2->svg d2 :layout layout :theme theme :scale scale :dark-theme dark-theme
             :sketch sketch :pad pad :center center :animate-interval animate-interval)))


(defn- image [opts]
  (let [svg (image-impl opts (handle-in (or (:image opts) (:i opts))))
        output-file (or (:output opts) (:o opts))]
    (if output-file
      (spit output-file svg)
      (println svg))))


(defn- apply-template-watch [opts]
  (when-not (or (:template opts) (:t opts))
    (exception "An --template/-t file should be specified."))
  (when-not (or (:output opts) (:o opts))
    (exception "An --output/-o file should be specified."))
  (let [path (or (:watch opts) (:w opts))
        tmp-path (or (:template opts) (:t opts))
        out-path (or (:output opts) (:o opts))
        f (fn [] (with-open [out-data (io/writer out-path)]
                   (binding [*out* out-data]
                     (apply-template-impl opts (slurp path)))))]
    (f) ;; do once when first called
    (fw/add-watch path f)
    (when tmp-path (fw/add-watch tmp-path f))
    @(promise)))


(defn- graph-image-watch [opts]
    (generic-image-watch graph-transform opts))


(defn- graph-image-impl [opts in]
  (when-not (installed? path-to-d2)
    (exception "d2 does not appear to be installed on your path."))
  (let [[_ grph] (read-data in)
        template-file (or (:template opts) (:t opts))
        template (when template-file (second (read-data (slurp template-file))))
        grph (if template (merge grph template) grph)
        dict (g/graph-spec->dictim grph)
        d2 (compile-fn dict)
        d2-opts (extract-d2-opts opts)]
    (d2->svg d2 d2-opts)))


(defn- graph-image [opts]
  (let [svg (graph-image-impl opts (handle-in (or (:graph opts) (:g opts))))
        output-file (or (:output opts) (:o opts))]
    (if output-file
      (spit output-file svg)
      (println svg))))


(defn- graph-impl [opts in]
  (let [[_ grph] (read-data in)]
    (try
      (let [template-file (or (:template opts) (:m opts))
            template (when template-file
                       (try
                         (second (read-data (slurp template-file)))
                         (catch java.io.FileNotFoundException ex
                           (exception (str "Template file not found: " template-file)))
                         (catch Exception ex
                           (exception (str "Error reading template file " template-file ": " (.getMessage ex))))))
            grph (merge grph template)
            dict (g/graph-spec->dictim grph)]
        (parse-print opts dict))
      (catch Exception ex
        (exception (.getMessage ex))))))


(defn- graph [opts]
  (graph-impl opts (handle-in (or (:graph opts) (:g opts)))))


(defn- graph-watch [opts]
  (when-not (or (:output opts) (:o opts))
    (exception "An --output/-o file should be specified."))
  (let [path (or (:watch opts) (:w opts))
        tmp-path (or (:template opts) (:t opts))
        out-path (or (:output opts) (:o opts))
        f (fn [] (with-open [out-data (io/writer out-path)]
                   (binding [*out* out-data]
                     (graph-impl opts (slurp path)))))]
    (f) ;; do once when first called
    (fw/add-watch path f)
    (when tmp-path (fw/add-watch tmp-path f))
    @(promise)))


(def ^:private version
  (str/trim (slurp (io/file "resources" "VERSION"))))



(def ^:private d2_version
  (str/trim (slurp (io/file "resources" "D2_VERSION"))))


(defn- dictim-flat-print [opts dict]
  (cond-> dict
    (:j opts)            (-> (json/to-json {:pretty (:m opts)}) println)

    (not (:j opts))      pp/pprint))


(defn- flatten*-impl [opts in]
  (let [[_ dict] (read-data in)]
    (dictim-flat-print opts (apply flat/flatten dict))))


(defn- flatten* [opts]
  (flatten*-impl opts (handle-in (or (:flatten opts) (:f opts)))))


(defn- dictim-build-print [opts dict]
  (if (:j opts)
    (-> (json/to-json {:pretty (:m opts)}) println)

    (cond
      (:k opts)   (pp/pprint (apply walk/keywordize-keys dict))
      (:s opts)   (pp/pprint (apply walk/stringify-keys dict))
      :else       (pp/pprint dict))))


(defn- build-impl [opts in]
  (let [[_ flatdict] (read-data in)]
    (dictim-build-print opts (-> flatdict flat/build))))


(defn- build [opts]
  (build-impl opts (handle-in (or (:build opts) (:b opts)))))


(defn- keywordize-impl [opts in]
  (let [[_ dict] (read-data in)]
    (if (v/all-valid? dict :d2)
      (pp/pprint (apply walk/keywordize-keys dict))
      (exception "supplied dictim is not valid."))))


(defn- keywordize [opts]
  (keywordize-impl opts (handle-in (or (:keywordize opts) (:k opts)))))


(defn- stringify-impl [opts in]
  (let [[_ dict] (read-data in)]
    (if (v/all-valid? dict :d2)
      (pp/pprint (apply walk/stringify-keys dict))
      (exception "supplied dictim is not valid."))))


(defn- stringify [opts]
  (stringify-impl opts (handle-in (or (:stringify opts) (:st opts)))))


(defn- validate-impl [opts in]
  (let [[_ dict] (read-data in)]
    (println (v/all-valid? dict :d2))))


(defn- validate [opts]
  (validate-impl opts (handle-in (or (:validate opts) (:val opts)))))


(defn- bad-watch-file?
  "Watch option, but file to be watched doesn't exist?"
  [opts]
  (or
   (and (or (:c opts) (:compile opts)) (:w opts) (not (fs/exists? (:w opts))))
   (and (or (:p opts) (:parse opts)) (:w opts) (not (fs/exists? (:w opts))))
   (and (or (:i opts) (:image opts)) (:w opts) (not (fs/exists? (:w opts))))
   (and (or (:a opts) (:apply-tmp opts)) (:w opts) (not (fs/exists? (:w opts))))
   (and (or (:g opts) (:graph opts)) (:w opts) (not (fs/exists? (:w opts))))
   (and (:cw opts) (not (fs/exists? (:cw opts))))
   (and (:pw opts) (not (fs/exists? (:pw opts))))
   (and (:iw opts) (not (fs/exists? (:iw opts))))
   (and (:aw opts) (not (fs/exists? (:aw opts))))
   (and (:gw opts) (not (fs/exists? (:gw opts))))))


(defn -main
  [& args]
  (let [opts (cli/parse-opts args cli-spec)]
    (try
      (cond
        (bad-watch-file? opts)
        (exception "File doesn't exist. NB: don't use stdin redirection ('<') with watch.")
        
        (and (or (:keywordize opts) (:k opts))
             (or (:stringify opts) (:s opts)))
        (exception "-k/--keywordize and -s/--stringify are mutually exclusive.")
        
        (or (:help opts) (:h opts))
        (println (show-help cli-spec))

        (or (:version opts) (:v opts))
        (println (str "Version: " version "  (d2 compatibility: " d2_version ")"))

        (:cw opts)
        (compile-watch (assoc opts :c true :w (:cw opts)))

        (:pw opts)
        (parse-watch (assoc opts :p true :w (:pw opts)))

        (:iw opts)
        (do (image-watch (assoc opts :i true :w (:iw opts))) @(promise))

        (:aw opts)
        (apply-template-watch (assoc opts :a true :w (:aw opts)))

        (:gw opts)
        (graph-watch (assoc opts :g true :w (:gw opts)))

        (:giw opts)
        (do (graph-image-watch (assoc opts :g true :i true :w (:giw opts))) @(promise))

        (and (or (:graph opts) (:g opts))
             (or (:image opts) (:i opts))
             (or (:watch opts) (:w opts)))
        (do (graph-image-watch opts) @(promise))

        (and (or (:graph opts) (:g opts))
             (or (:watch opts) (:w opts)))
        (graph-watch opts)
        
        (and (or (:compile opts) (:c opts))
             (or (:watch opts) (:w opts)))
        (compile-watch opts)

        (and (or (:image opts) (:i opts))
             (or (:watch opts) (:w opts)))
        (do (image-watch opts) @(promise))


        (or (:compile opts) (:c opts))
        (compile opts)

        (and (or (:parse opts) (:p opts))
             (or (:watch opts) (:w opts)))
        (parse-watch opts)

        (or (:parse opts) (:p opts))
        (parse opts)

        (and (or (:apply-tmp opts) (:a opts))
             (or (:watch opts) (:w opts)))
        (apply-template-watch opts)

        (or (:apply-tmp opts) (:a opts))
        (apply-template opts)

        (and (or (:graph opts) (:g opts))
             (or (:image opts) (:i opts)))
        (graph-image opts)

        (or (:graph opts) (:g opts))
        (graph opts)

        (or (:image opts) (:i opts))
        (image opts)

        (or (:flatten opts) (:f opts))
        (flatten* opts)

        (or (:build opts) (:b opts))
        (build opts)

        (or (:keywordize opts) (:k opts))
        (keywordize opts)

        (or (:stringify opts) (:st opts))
        (stringify opts)

        (or (:validate opts) (:val opts))
        (validate opts)

        :else
        (println (str "Unknown option. Please consult the help with the -h flag.")))
      (catch Exception ex
        (println (.getMessage ex)))
      (finally (System/exit 0)))))
