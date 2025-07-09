(ns
    ^{:author "judepayne"
      :doc "Namespace for validating dictim templates and graphspecs."}
    dictim.spec
  (:require [malli.core :as m]
            [malli.error :as me]
            [dictim.graph.core :as g]
            [dictim.tests :as t]
            [dictim.validate :as v]
            [dictim.utils :refer [error]])
  (:refer-clojure :exclude [test]))


;; a dynamic var to hold whether :d2 or :dot is the output format.
(def ^:dynamic ^:private output :d2)


(def keys-to-keywordize #{"src" "dest" "nodes" "edges" "node->key" "node-template" "edge-template"
                          "node->container" "container->parent" "container-template" "container->data"
                          "template" "directives"})

(def vals-to-vector #{:nodes :edges :node-template :container-template :edge-template :template})

(defn keywordize-select-keys
  [m keys-to-keywordize]
  (letfn
      [(walk [x]
         (cond
           (map? x)
           (into {}
                 (map (fn [[k v]]
                        (let [k' (if (and (string? k) (contains? keys-to-keywordize k))
                                   (keyword k)
                                   k)
                              v' (if (contains? vals-to-vector k')
                                   (into [] v)
                                   v)]
                          [k' (walk v')]))
                      x))

           (sequential? x)
           (mapv walk x)

           :else x))]
      (walk m)))


(defn normalize
  "keywordizes selected keys to make spec definition simpler"
  [graph-spec]
  (keywordize-select-keys graph-spec keys-to-keywordize))


(defn validate-d2-attrs
  "validates d2 attrs"
  [attrs]
  (v/valid-attrs? attrs output :template))


(def kstr [:or :string :keyword])

(def node->key kstr)

(def node [:map-of :any :any])

(def edge
  [:and
   [:map-of :any :any] ; open map with any extra fields
   [:fn {:error/message "edge must contain :src and :dest keys"}
    (fn [m]
      (and (contains? m :src)
           (contains? m :dest)))]])

;; Created single validation function for test-clauses, because with
;; custom validation, malli stops on the first error. This way collects
;; all errors
(defn- validate-test-clauses
  [clauses]
  (let [pairs (partition 2 clauses)]
    (reduce
     (fn [acc [test attr]]
       (and acc
            (t/valid-test? test)
            (try
              (validate-d2-attrs attr)
              (catch Exception e false))))
     true
     pairs)))

(defn- test-clauses-errors
  [clauses]
  (let [pairs (partition 2 clauses)
        errors
        (->> (reduce
              (fn [acc [test attr]]
                (let [test-error (when (not (t/valid-test? test)) (str test " is an invalid test."))
                      attr-error (try
                                   (validate-d2-attrs attr)
                                   nil
                                   (catch Exception e
                                     (.getMessage e)))]
                  (conj acc test-error attr-error)))
              '()
              pairs)
             (filter (complement nil?)))]
    (apply str (interpose "\n" errors))))

(def test-clauses
  [:fn {:error/fn (fn [{:keys [value]} _] (test-clauses-errors value))}
   validate-test-clauses])

(def node->container kstr)

(def container->parent [:map-of :any :any])

(def container->data [:map-of :any :any])

(def directives
  [:fn {:error/fn (fn [{:keys [value]} _] (str "Invalid directives: " value))}
   validate-d2-attrs])


(def ^{:private true} gs
  [:and
   [:map
    [:node->key kstr]
    [:nodes [:sequential node]]
    [:edges {:optional true} [:sequential edge]]
    [:node-template {:optional true} test-clauses]
    [:edge-template {:optional true} test-clauses]
    [:container-template {:optional true} test-clauses]
    [:node->container {:optional true} node->container]
    [:container->data {:optional true} container->data]
    [:template {:optonal true} test-clauses]
    [:directives {:optional true} directives]]

   [:fn {:error/message "node->key must exist in every node"}
    (fn [{:keys [nodes node->key]}]
      (every? #(contains? % node->key) nodes))]])


(def graph-spec (m/validator gs))


(def template-spec (m/validator test-clauses))


(defn- hu [spec data]
  (me/humanize (m/explain spec data)))


(defn- -spec-errors
  "Returns validation errors in easy to read form."
  [malli-spec data]
  (hu malli-spec data))

(defn- spec-errors
  "Returns a formatted string of validation errors for command line display."
  [spec data]
  (when-let [errors (-spec-errors spec data)]
    (let [error-msg
          (cond
            (vector? errors)
            (apply str (interpose "\n" errors))

            (map? errors)
            (apply str (interpose "\n"
                                  (for [[k v] errors]
                                    (if (vector? v)
                                      (apply str (interpose "\n" v))
                                      (str v)))))

            :else
            (str errors))]
      (str "Validation failed:\n" error-msg))))


(defn validate-graphspec
  "Validates a graphspec. Returns either true or throws an exception with a 
   meaingful error messaage of validation errors."
  [spec output-format]
  (binding [output output-format]
    (let [spec (normalize spec)]
      (if (graph-spec spec)
        true
        (throw (ex-info (spec-errors gs spec)
                        {:type :graphspec-validation-error}))))))


(defn validate-template
  "Validates a template. Returns either true of throws an exception with a
   meaingful error message of validation errors."
  [template output-format]
  (binding [output output-format]
    (if (template-spec template)
      true
      (throw (ex-info (spec-errors test-clauses template)
                      {:type :template-validation-error})))))
