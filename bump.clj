#!/usr/bin/env bb

;; Script to git add *, then commit with a commit msg supplied as a first arg
;; tags the new commit with an updated version
;; update the README to the latest version tag and sha
;; and push the whole thing to github

(require '[babashka.process :refer [shell sh process check]]
         '[clojure.tools.cli :refer [parse-opts]]
         '[clojure.string :as str])

(defn version-map [p]
  (let [text
        (-> (shell {:out :string} (str "git -C " p  " describe --tags --abbrev=0")) :out str/trim)]
    (zipmap [:major :minor :patch] (map #(Integer/parseInt %) (str/split text #"\.")))))

(defn bump-version [{major :major
                     minor :minor
                     patch :patch}
                    k]
  (case k
    :major   (str (inc major) ".0.0")
    :minor   (str major "." (inc minor) ".0")
    :patch   (str major "." minor "." (inc patch))))

(defn current-sha [p]
  (-> (shell {:out :string} (str "git -C " p  " rev-parse --short HEAD")) :out str/trim))

(def cli-options
  [["-v" "--version VERSION"
    :default :patch
    :parse-fn keyword
    :validate [#(contains? #{:major :minor :patch} %) "Must be major, minor or patch"]]
   ["-p" "--path PATH"
    :default "."]
   ["-h" "--help"]])

(defn arguments []
  (let [args (parse-opts *command-line-args* cli-options)]
    (if (:errors args)
      (do
        (println (:errors args))
        (System/exit 1))
      {:args (apply str (interpose " " (:arguments args)))
       :opts (:options args)})))

(defn update-sha [text sha]
        (str/replace text #"(?<=:git/sha )\"(.*?)\"" (str "\"" sha "\"")))

(defn update-tag [text tag]
        (str/replace text #"(?<=:git/tag )\"(.*?)\"" (str "\"" tag "\"")))


(defn -main []
  (let [args (arguments)
        path (-> args :opts :path)
        next-version (bump-version (version-map path) (-> args :opts :version))]
    (shell (str "git -C " path " add *"))
    (shell (str "git -C " path " commit -m '" (-> args :args) "'"))
    (let [sha (current-sha path)
          readme (-> (slurp "README.md")
                     (update-sha sha)
                     (update-tag next-version))]
      (spit "README.md" readme)
      (shell (str "git -C " path " tag " next-version " " sha))
      (shell (str "git -C " path " add README.md"))
      (shell (str "git -C " path " commit -m 'bump version'"))
      (shell (str "git -C " path " push origin main"))
      (shell (str "git -C " path " push --tags")))))


(-main)

