(ns release-artifact
  (:require
   [borkdude.gh-release-artifact :as ghr]
   [borkdude.gh-release-artifact.internal :as ghr-internal]
   [babashka.http-client :as http]
   [cheshire.core :as cheshire]
   [clojure.java.shell :refer [sh]]
   [clojure.string :as str]))

(defn current-branch []
  (or (System/getenv "APPVEYOR_PULL_REQUEST_HEAD_REPO_BRANCH")
      (System/getenv "APPVEYOR_REPO_BRANCH")
      (System/getenv "CIRCLE_BRANCH")
      (System/getenv "GITHUB_REF_NAME")
      (System/getenv "CIRRUS_BRANCH")
      (-> (sh "git" "rev-parse" "--abbrev-ref" "HEAD")
          :out
          str/trim)))

(defn extract-release-notes [version]
  (let [changelog (slurp "CHANGELOG.md")
        pattern (re-pattern (str "(?s)#\\s+" (java.util.regex.Pattern/quote version) "\\s*\\n(.*?)(?=\\n#\\s|\\z)"))
        match (re-find pattern changelog)]
    (when match (str/trim (second match)))))

(defn set-release-notes [org repo tag notes]
  (let [token (System/getenv "GITHUB_TOKEN")
        release (ghr-internal/release-for {:org org :repo repo :tag tag})
        release-id (:id release)
        url (str "https://api.github.com/repos/" org "/" repo "/releases/" release-id)]
    (http/patch url
                {:headers {"Authorization" (str "token " token)
                           "Accept" "application/vnd.github.v3+json"
                           "Content-Type" "application/json"}
                 :body (cheshire/generate-string {:body notes})})))

(defn release [{:keys [file]}]
  (let [ght (System/getenv "GITHUB_TOKEN")
        _ (when ght (println "Github token found"))
        _ (println "File" file)
        branch (current-branch)
        _ (println "On branch:" branch)
        current-version
        (-> (slurp "resources/VERSION")
            str/trim)]
    (if (and ght (contains? #{"master" "main"} branch))
      (do (assert file "File name must be provided")
          (println (str "On main branch. Publishing asset. " current-version))
          (ghr/overwrite-asset {:org "judepayne"
                                :repo "dictim"
                                :file file
                                :tag current-version
                                :draft false
                                :overwrite true
                                :sha256 true})
          (when-let [notes (extract-release-notes current-version)]
            (println "Setting release notes...")
            (set-release-notes "judepayne" "dictim" current-version notes)))
      (println "Skipping release artifact (no GITHUB_TOKEN or not on main branch)"))
    nil))
