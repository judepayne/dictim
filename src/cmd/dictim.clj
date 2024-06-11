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
            [babashka.fs :as fs]
            [babashka.process :refer [shell]])
  (:refer-clojure :exclude [compile])
  (:gen-class))


(defn- exception [msg]
  (throw (Exception. msg)))


(defn show-help
  [spec]
  (cli/format-opts (merge spec {:order [:compile :parse :k :j :b :watch :layout :theme :d :scale :version :help]})))


(def compile-help
  "Compiles supplied dictim to d2
                The value supplied to --compile may be either
                  - a edn/ json dictim syntax string (in single quotes)
                  - ommitted in which case *std-in* is read")


(def parse-help
  "Parses supplied d2 into dictim
                The value supplied to --parse may be either
                  - a d2 string (in single quotes)
                  - ommitted in which case *std-in* is read
                --parse has various supplemental flags:")


(def watch-help
  "Watches an edn/ json dictim syntax file and serves the resultant diagram in your default browser.
                (watch requires d2 to be installed and available on your path)
                watch has three sub option d2 settings, layout, theme and d (debug):")


(def cli-spec
  {:spec
   {:compile {:desc compile-help
              :alias :c}
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
    :layout {:desc "d2 layout engine name; dagre/ elk/ tala"
             :alias :l}
    :theme {:desc "d2 theme id. See https://d2lang.com/tour/themes"
            :alias :t}
    :d {:coerce :boolean
        :desc "debug for Watch: Shows interim d2 in the browser."}
    :scale {:desc "determines the svg scaling factor used by d2. default is 1.2"
            :alias :s}
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
      (exception "Error: data format does not appear to be either edn or json"))))


(defn- compile-fn [dict]
  (cond
    (empty? dict)
    (exception "Error: no dictim to compile")

    (or dict (and (list? dict) (every? coll? dict)))
    (if (list? dict)
      (apply c/d2 dict) 
      (c/d2 dict))
      

    :else
    (exception "Error: Could not read input as valid dictim syntax")))


(defn- compile [opts]
  (let [input (handle-in (or (:compile opts) (:c opts)))
        [_ dict] (read-data input)]
    (-> dict compile-fn println)))


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


(def d2-default-scale 1.2)


;; correct command line is:   echo "x -> y: hello" | d2 --layout tala -
(defn d2->svg
  "Takes a string of d2, and returns a string containing SVG."
  [d2 & {:keys [layout theme scale]
         :or {layout d2-default-layout
              theme d2-default-theme
              scale d2-default-scale}}]
  (let [{:keys [out err]} (shell {:out :string :in d2} "d2" "--layout" layout
                                 "--theme" theme "--scale" scale  "-")]
    (or
     out
     (str "Error: d2 engine error: "(format-error d2 err)))))


(defn transform [debug? d2-opts file-contents]
  (try
    (let [[_ dict] (read-data file-contents)
          d2 (compile-fn dict)]
      (html [:div (d2->svg d2 d2-opts)]
            [:div (when debug? (str "<b>d2:</b><br>" (str/replace d2 #"\n" "<br>")))]))
    (catch Exception ex
      (.getMessage ex))))


(defn- watch [opts]
  (let [file (or (:watch opts) (:w opts))
        layout (or (:layout opts) (:l opts))
        theme (or (:theme opts) (:t opts))
        scale (or (:scale opts) (:s opts))
        debug? (or (:d opts) false)
        d2-opts (cond-> nil
                  layout (assoc :layout layout)
                  theme  (assoc :theme theme)
                  scale  (assoc :scale scale))]
    (cond
      (and (fs/exists? file) (installed? path-to-d2))
      (serve/start file (partial transform debug? d2-opts))

      (fs/exists?) (exception "Error: d2 does not appear to be installed on your path.")
      
      :else (exception "Error: File does not exist"))))


(defn- parse [opts]
  (cond-> (handle-in (or (:parse opts) (:p opts)))
    (:k opts)            (p/dictim :key-fn keyword)
    (not (:k opts))      p/dictim

    (:r opts)            tmp/remove-styles

    (:j opts)            (-> (json/to-json {:pretty (:b opts)}) println)

    (not (:j opts))      pp/pprint))


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

        (or (:compile opts) (:c opts))
        (compile opts)

        (or (:parse opts) (:p opts))
        (parse opts)

        (or (:watch opts) (:w opts))
        (do (watch opts) @(promise))

        :else
        (println (str "Error: Unknown option\n" (show-help cli-spec))))
      (catch Exception ex
        (println (str "Error: " (.getMessage ex))))
      (finally (System/exit 0)))))
