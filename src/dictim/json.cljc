(ns ^{:author "judepayne"
      :doc "Namespace for common functions."}
    dictim.json
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            #?(:clj [instaparse.core :as insta :refer [defparser]]
               :cljs [instaparse.core :as insta :refer-macros [defparser]])))


;; serialization/ deserialization of dictim to json


(defn to-json
  "Serializes the dictim to json."
  [dictim & {:as opts}]
  (json/generate-string dictim opts))


;; The Json std does not allow arrays or maps as keys of maps.
;; This is a problem for dictim which has conn-refs as keys of maps.
;; We'll have to do some posting processing to attempt to parse
;; map keys as conn-refs.

(defn- remove-quoting [s]
  (str/replace s #"\"" ""))


(def ^:private conn-ref-grammar
  "conn-ref-key = <'['> <s> crk <s> dir <s> crk <s> <'['> <s> array-val <s> <']'> <']'>
    crk = #'((?!(?:<-|->|<->|--))[^.;:\\n{}<>&()\\[\\]])+'
    array-val = #'\\d' | '*'+
    dir = '--' | '->' | '<-' | '<->'
    s = #' *'")


(defparser ^{:doc "A parser for d2" :private true} parse-d2 conn-ref-grammar)


(def conn-ref-reg
  (re-pattern
   (str "\\[\\s*"
        "((?!(?:<-|->|<->|--))[^.;:\n{}<>&()\\[\\]])+"
        "(?:<-|->|<->|--)"
        "((?!(?:<-|->|<->|--))[^.;:\n{}<>&()\\[\\]])+"
        "\\[\\\".\\\"\\]\\]")))


(defn conn-ref? [x] (when (re-matches conn-ref-reg x) true))


(defn- parse-conn-ref-key
  [maybe-key]
  (let [p-tree (parse-d2 (remove-quoting maybe-key))]
    (if (insta/failure? p-tree)
      maybe-key ;;couldn't parse. just return the key as it was
      (insta/transform
       {:crk str/trim
        :dir identity
        :array-val identity
        :conn-ref-key (fn [k1 d k2 arv]
                        [k1 d k2 [arv]])}
       p-tree))))


(defn convert-conn-ref-key
  [maybe-key]
  (if (conn-ref? maybe-key)
    (parse-conn-ref-key maybe-key)
    maybe-key))


;; This fn is public as might be used outside of 'from-json' e.g. in dictim.server
;; when the serialization is done by the web framework.
(defn fix-conn-ref-keys
  [m]
  (clojure.walk/postwalk
   (fn [x]
     (if (map? x)
       (reduce
        (fn [m [k v]]
          (assoc m (convert-conn-ref-key k) v))
        {}
        x)
       x))
   m))


;; This fix for connection reference key has a performance penalty. This is optimized
;; by doing a regex check before attempting to parse

(defn from-json
  "Desrializes the json to dictim"
  [js & args]
  (-> (apply json/parse-string js args)
      fix-conn-ref-keys))
