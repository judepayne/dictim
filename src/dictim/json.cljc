(ns ^{:author "judepayne"
      :doc "Namespace for converting dictim to/ from json."}
    dictim.json
  (:require [cheshire.core :as json]))


;; serialization/ deserialization of dictim to json

(defn to-json
  "Serializes the dictim to json."
  [dictim & {:as opts}]
  (json/generate-string dictim opts))


(defn- vector-in-vector? [coll]
  (and (vector? coll)
       (vector? (first coll))))


(defn from-json
  "Desrializes the json to dictim"
  [js & args]
  ;;parse-string-strict avoids lazy parsing. part of the 'unofficial' cheshire api
  (let [js (apply json/parse-string-strict js args)]
    (if (vector-in-vector? js)
      (apply list js)
      js)))
