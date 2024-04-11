(ns ^{:author "judepayne"
      :doc "Namespace for common functions."}
    dictim.json
  (:require [cheshire.core :as json]))


;; serialization/ deserialization of dictim to json


(defn to-json
  "Serializes the dictim to json."
  [dictim & {:as opts}]
  (json/generate-string dictim opts))


(defn from-json
  "Desrializes the json to dictim"
  [js & args]
  (apply json/parse-string js args))
