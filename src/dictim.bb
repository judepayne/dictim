#!/usr/bin/env bb

;; babashka script for the cmd line tool

(ns dictim
  (:require [dictim.d2.compile :as c]
            [dictim.d2.parse :as p]
            [dictim.json :as json]
            [babashka.cli :as cli]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.edn :as edn]))


(defn show-help
  [spec]
  (cli/format-opts (merge spec {:order (vec (keys (:spec spec)))})))


;; pretty printing of json and d2. dictim as well?


(def cli-spec
  {:spec
   {:compile {:desc "Compiles supplied dictim to d2"
              :alias :c}
    :parse {:desc "Parses supplied d2 into dictim"
            :alias :p}
    :version {:coerce :boolean
              :desc "Returns the version of dictm"
              :alias :v}
    :help {:coerce :boolean
           :desc "Displays this help"
           :alias :h}
    :k {:coerce :boolean
        :desc "Convert keys to keywords"}
    :j {:coerce :boolean
        :desc "Converts the output of parse to json"}
    :beautify {:coerce :boolean
               :alias :b
               :desc "Pretty print the output"}}
   :error-fn
   (fn [{:keys [spec type cause msg option] :as data}]
     (if (= :org.babashka/cli type)
       (case cause
         :require
         (println
          (format "Missing required argument: %s\n" option))
         :validate
         (println
          (format "%s does not exist!\n" msg)))))})


(defn- from-json [maybe-json]
  (try
    (when-let [dict (doall (json/from-json maybe-json))]
      dict)
    (catch java.io.IOException ex (do (.getName (class ex)) nil))))


(defn- from-edn [maybe-edn]
  (try
    (when-let [dict (edn/read-string (str/replace maybe-edn #"\\n" ""))]
      dict)
    (catch Exception ex (do (.getName (class ex)) nil))))


(defn- to-json [dict & {:keys [pretty?] :or {pretty? false}}]
  (if (not pretty?)
    (json/to-json dict {:pretty true})
    (json/to-json dict)))


(defn- pretty-d2 [d2]
  (let [lines (str/split d2 #"\n")]
    (mapv println lines)))


(defn- handle-in [arg]
  (if (str/starts-with? arg "@")
    (try
      (slurp (subs arg 1))
      (catch Exception ex
        (throw (Exception. (str "Can't open file " arg)))))
    arg))


(defn- beautify? [opts]
  (or (:beautify opts) (:b opts)))


(defn- compile [opts]
  (let [input (handle-in (or (:compile opts) (:c opts)))
        dict
        (if-let [dict (from-json input)]
          dict
          (if-let [dict (from-edn input)]
            dict
            nil))]
    (cond
      (empty? dict) (println "Error: no dictim to compile")
      
      (or (and dict (not (list? dict))) (and (list? dict) (every? coll? dict)))
      (try
        (if (list? dict)
          (let [d2 (apply c/d2 dict)] (println d2))
          (let [d2 (c/d2 dict)] (println d2)))
        (catch Exception ex
          (.getMessage ex)))
      
      :else (println "Error: Could not read input as valid json or dictim"))))


(defn- parse [opts]
  (try
    (let [d2 (handle-in (or (:parse opts) (:p opts)))
          dict (if (:k opts)
                 (p/dictim d2 :key-fn keyword)
                 (p/dictim d2))]
      (println (if (:j opts) (to-json dict {:pretty (beautify? opts)}) (apply str dict))))
    (catch Exception ex
      (println (str "Error: "
                    (.getMessage ex))))))


(defn -main
  [& args]
  (let [opts (cli/parse-opts args cli-spec)]
    (try
      (cond
        (or (:help opts) (:h opts))
        (println (show-help cli-spec))

        (or (:version opts) (:v opts))
        (println (str/trimr (slurp (io/resource "VERSION"))))

        (or (:compile opts) (:c opts))
        (compile opts)

        (or (:parse opts) (:p opts))
        (parse opts)

        :else
        (println (str "Error: Unknown option\n" (show-help cli-spec))))
      (catch Exception ex
        (println (str "Error: " (.getMessage ex)))))))


#_(-main *command-line-args*)
