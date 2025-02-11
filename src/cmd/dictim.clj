;; babashka script for the cmd line tool
(ns cmd.dictim
  (:require [dictim.d2.compile :as c]
            [dictim.d2.parse :as p]
            [dictim.json :as json]
            [dictim.template :as tmp]
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
  (throw (Exception. msg)))


(defn show-help
  [spec]
  (cli/format-opts (merge spec {:order [:compile :parse :k :j :b :watch :layout :theme :d :scale :apply-tmp :template :graph :version :help]})))


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

                   --parse has various other supplemental flags:")


(def watch-help
  "When used on its own, Watches an edn/ json dictim syntax file
                   and serves the resultant diagram in the default browser.
                   watch requires d2 to be installed and available on your
                   path. watch has sub option settings:
                   (d2) layout, (d2) theme, scale and debug:")


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


(def graph-help
  "Converts a dictim graph spec to dictim.
                   applies a dictim template specified via the
                   --template/ -t option.
                   -j and -b options are also available.")


(def cli-spec
  {:spec
   {:compile {:desc compile-help
              :alias :c}
    :template {:desc "Path to an edn/ json template file"
               :alias :t}
    :parse {:desc parse-help
            :alias :p}
    :k {:coerce :boolean
        :desc "Convert keys to keywords when parsing d2 to dictim syntax edn"}
    :j {:coerce :boolean
        :desc "Converts the output of parse to dictim syntax json"}
    :b {:coerce :boolean
        :desc "Additional to  -j: prettifies the json output of parse"}
    :r {:coerce :boolean
        :desc "Removes styles (attributes) from parsed d2, including any vars"}
    :watch {:desc watch-help
            :alias :w}
    :output {:desc ""
             :alias :o}
    :layout {:desc "d2 layout engine name; dagre/ elk/ tala"
             :alias :l}
    :theme {:desc "d2 theme id. See https://d2lang.com/tour/themes"
            :alias :th}
    :d {:coerce :boolean
        :desc "debug for Watch: Shows interim d2 in the browser."}
    :scale {:desc "determines the svg scaling factor used by d2. default is 1.0"
            :alias :s}
    :apply-tmp {:desc apply-template-help
                :alias :a}
    :graph {:desc graph-help
            :alias :g}
    :version {:coerce :boolean
              :desc "Returns the version of dictm"
              :alias :v}
    :help {:coerce :boolean
           :desc "Displays this help"
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
    (when-let [dict (edn/read-string maybe-edn)]
      dict)
    (catch Exception ex (do (.getName (class ex)) nil))))


(defn- handle-in [arg]
  (cond
    (true? arg)      (slurp *in*)
     
    :else arg))


(defn- read-data [data]
  (if-let [dict (from-json data)]
    [:json dict]
    (if-let [dict (from-edn data)]
      [:edn dict]
      (exception "data format does not appear to be either edn or json"))))


(defn- compile-fn [dict]
  (cond
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


(def d2-default-scale 1.0)


(defn- d2-args [d2-opts]
  (let [m (into {} (filter #(-> % val some?) d2-opts))]
    (flatten
     (mapv
      (fn [[k v]]
        [(str "--" (name k)) (str v)])
      m))))


;; correct command line is:   echo "x -> y: hello" | d2 --layout tala -
(defn d2->svg
  "Takes a string of d2, and returns a string containing SVG."
  [d2 & {:keys [layout theme scale]
         :or {layout d2-default-layout
              theme d2-default-theme
              scale d2-default-scale}
         :as d2-opts}]
  (let [shell-args (flatten [{:out :string :in d2} "d2" (d2-args d2-opts) "-"])
        {:keys [out err]} (apply shell shell-args)]
    (or
     out
     (str "d2 engine error: "(format-error d2 err)))))


(defn transform
  ([debug? d2-opts file-contents]
   (transform debug? d2-opts file-contents nil))
  
  ([debug? d2-opts file-contents template-contents]
   (try
     (let [[_ dict] (read-data file-contents)
           [_ tmp] (when template-contents (read-data template-contents))
           d2-opts (if template-contents (dissoc d2-opts :layout :theme) d2-opts)
           dict (if template-contents (apply-dictim-template dict tmp) dict)
           d2 (compile-fn dict)]
       (html [:div (d2->svg d2 d2-opts)]
             [:div (when debug? (str "<b>d2:</b><br>" (str/replace d2 #"\n" "<br>")))]))
     (catch Exception ex
       (.getMessage ex)))))


(defn- watch [opts]
  (let [file (or (:watch opts) (:w opts))
        layout (or (:layout opts) (:l opts))
        theme (or (:theme opts) (:th opts))
        template (or (:template opts) (:t opts))
        scale (or (:scale opts) (:s opts))
        debug? (or (:d opts) false)
        d2-opts (cond-> nil
                  layout (assoc :layout layout)
                  theme  (assoc :theme theme)
                  scale  (assoc :scale scale))]
    (cond
      (and template (fs/exists? template) (fs/exists? file) (installed? path-to-d2))
      (serve/start (partial transform debug? d2-opts) file template)
      
      (and (fs/exists? file) (installed? path-to-d2))
      (serve/start (partial transform debug? d2-opts) file)

      (fs/exists? file) (exception "d2 does not appear to be installed on your path.")
      
      :else (exception "File does not exist"))))


(defn- parse-print [opts dict]
  (cond-> dict
    (:j opts)            (-> (json/to-json {:pretty (:b opts)}) println)

    (not (:j opts))      pp/pprint))


(defn- parse-impl [opts in]
  (cond-> in
    (:k opts)            (p/dictim :key-fn keyword)
    (not (:k opts))      p/dictim

    (:r opts)            tmp/remove-attrs))


(defn- parse [opts]
  (->>
   (parse-impl opts (handle-in (or (:parse opts) {:p opts})))
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


(defn- keywordize [m]
  (clojure.walk/postwalk
   (fn [x]
     (if (map? x)
       (into {} (map (fn [[k v]] [(keyword k) v]) x))
       x))
   m))

(defn- keywordize1 [m]
  (into {} (map (fn [[k v]] [(keyword k) v]) m)))


(defn- graph-impl [opts in]
  (let [[_ grph] (read-data in)
        dict (g/graph-spec->dictim grph)
        template-file (or (:template opts) (:m opts))
        template (when template-file (second (read-data (slurp template-file))))]
    (if template
      (parse-print opts (apply-dictim-template dict template))
      (parse-print opts dict))))


(defn- graph [opts]
  (graph-impl opts (handle-in (or (:graph opts) (:g opts)))))


(def ^:private version
  (str/trim (slurp (io/file "resources" "VERSION"))))


(defn -main
  [& args]
  (let [opts (cli/parse-opts args cli-spec)]
    (try
      (cond
        (or (:help opts) (:h opts))
        (println (show-help cli-spec))

        (or (:version opts) (:v opts))
        (println version)

        (and (or (:compile opts) (:c opts))
             (or (:watch opts) (:w opts)))
        (compile-watch opts)
        
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

        (or (:graph opts) (:g opts))
        (graph opts)

        (or (:watch opts) (:w opts))
        (do (watch opts) @(promise))        

        :else
        (println (str "Error: Unknown option\n" (show-help cli-spec))))
      (catch Exception ex
        (println (str "Error: " (.getMessage ex))))
      (finally (System/exit 0)))))
