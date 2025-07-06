(ns dictim.graphspec
  (:require [dictim.graph.core :as g]
            [dictim.tests :as t]
            [dictim.utils :refer [error]]
            [dictim.template :as tm]
            [malli.core :as m]
            [malli.error :as me])
  (:refer-clojure :exclude [comparator test]))


;; *****************************************
;; *            validation                 *
;; *****************************************

(def keys-to-keywordize #{"src" "dest" "nodes" "edges" "node->key" "node-template" "edge-template"
                          "node->container" "container->parent" "container-template" "container->data"})

(def vals-to-vector #{:nodes :edges :node-template :container-template :edge-template})

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


(def kstr [:or :string :keyword])

(def node [:map-of :any :any])

(def edge
  [:and
   [:map-of :any :any] ; open map with any extra fields
   [:fn {:error/message "edge must contain :src and :dest keys"}
    (fn [m]
      (and (contains? m :src)
           (contains? m :dest)))]])

(def node->key kstr)

(def test
  [:fn {:error/fn (fn [{:keys [value]} _] (str value " is an invalid test"))}
   t/valid-test?])

(def attr-map
  [:map-of :any :any])  ;; permissive

(def test-clauses
  [:+ [:cat test attr-map]])

(def node->container kstr)

(def container->parent [:map-of :any :any])

(def container->data [:map-of :any :any])


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
    [:container->data {:optional true} container->data]]

   [:fn {:error/message "node->key must exist in every node"}
    (fn [{:keys [nodes node->key]}]
      (every? #(contains? % node->key) nodes))]])


(def graph-spec (m/validator gs))


(defn- -graph-spec-errors
  "Returns validation errors in easy to read form."
  [spec]
  (me/humanize (m/explain gs spec)))


(defn graph-spec-errors
  "Returns a formatted string of validation errors for command line display."
  [spec]
  (when-let [errors (-graph-spec-errors spec)]
    (let [format-error-list (fn [errors]
                              (if (vector? errors)
                                (clojure.string/join "; " errors)
                                (str errors)))
          error-lines (for [[field field-errors] errors]
                        (cond
                          (= field :malli/error)
                          (str "Validation: " (format-error-list field-errors))
                          
                          (keyword? field)
                          (str (name field) ": " (format-error-list field-errors))
                          
                          :else
                          (str field ": " (format-error-list field-errors))))]
      (str "Graph specification errors:\n"
           (clojure.string/join "\n" 
                                (map #(str "  • " %) error-lines))))))



;; *****************************************
;; *                Specs                  *
;; *****************************************


(defn- interpolate
  "Interpolates s with the values looked up in elem from keys, ks."
  [elem [s & ks]]
  (let [vs (map
            #(if (vector? %)
               (get-in elem %)
               (get elem %))
            ks)]
    (try
      (apply format s vs)
      (catch Exception e
        (throw (error (str "'" s "' could not be interpolated for node: \n"
                           elem)))))))


(defn- spec-fn [tests]
  (fn [elem]
    (let [m ((t/test-fn tests) elem)
          m' (into {}
                   (map
                    (fn [[k v]] (if (vector? v) [k (interpolate elem v)] [k v]))
                    m))]
      (if-let [lbl (get m' "label")]
        (assoc (dissoc m' "label") :label lbl)
        m'))))


(defn- get*
  "Like get but indifferent to whether k is a keyword or a string."
  [m k]
  (if (keyword? k)
    (or (k m) (get m (name k)))
    (or (get m k) (get m (keyword k)))))

;; *****************************************
;; *             Public API                *
;; *****************************************


(defn graph-spec->dictim
  "A higher level data api for producing dictim which models the data as a graph.
   Takes a graph spec and produces dictim.
   A graph-spec is a map which must have keys:
     :nodes  a sequence of nodes (each of which is an arbitrary (nested) map).
     :edges  a sequence of edges ( ditto ).
     :node->key  the key used to extract a unique value (the id) from each node.
   and optionally can have keys:
     :node-template  see below.
     :edge-template  ditto
     :container->attrs a map of the container (name) to a map of (e.g. d2) attributes *or*
     :container-template see below *and*
     :container->data a map of the container (name) to a map of data representing it.
     :node->container a key applied to each node to determine which container it is in
     :container->parent a map with containers mapped to their parent containers.
     :directives a map of dictim directives to be added to the dictim
     :template a default dictim template to be applied to the dictim (i.e. after
       node-template, edge-template & container-template have been applied *during*
       the formation of the dictim). attributes set by those templates are not
       overridden by the application of :template

   This function makes no assumptions about whether the of the diagram spec are strings
   or keywords (both work). Similarly keywords and strings both work in specifying the
   nodes and edges data. The only assumption is that the spec is internally consistent.
   e.g. if a node is a map of keywords, then node-template, node->container, etc will
   also  be specified using keywords.

   node-template, edge-template are each sequences of
   test to a map of attrs e.g. style.fill blue in the below example...
   ````
   :node-template [[\"=\" \"dept\" \"Equities\"] {\"label\" [\"This dept is %s\" \"dept\"], \"style.fill\" \"blue\"}
                    \"else\" {\"label\" [\"%s\" \"dept\"]}]
   ````
   The test and other instructions are the same as dictim.template.
   For any values in the returned map that require dyanmic values to be interpolated into
   the string from the element meeting the test, e..g. `label` in the example above, use a 
   vector with the string to be interpolated into in the first position followed by keys
   whose values should be looked up from the element and merged into the string. If the
   element is represented by a nested map, instead of a single key, use a vector of keys.

   Interpolation is done by java.util.Formatter.

   Note that `\"label\"`/ `:label` is a special key name, used to set the element's
   label in the final dictim. The value of label can be an interpolation instruction (e.g. a vector)
   as described above.

   All nodes, edges and containers (specified by :container->data) are arbitrary (nested) maps.
   The key names: 'key' 'label' 'attrs' 'children' 'keys' & 'element-type' must be avoided
   as they are reserved for dictim elements."
  [spec & {:keys [validate?] :or {validate? true}}]

  (when validate?
    (let [spec (normalize spec)]
      (when (not (graph-spec spec))
        (throw (ex-info (graph-spec-errors spec) {})))))

  (let [nodes (get* spec :nodes)
        edges (get* spec :edges)
        node->key (let [nk (get* spec :node->key)]
                    (fn [n]
                      (or (get n nk) (get n (keyword nk)))))
        node-fn (if-let [ns (get* spec :node-template)]
                  (spec-fn ns) (constantly nil))
        edge-fn (if-let [es (get* spec :edge-template)]
                  (spec-fn es) (constantly nil))
        node->container (let [nc (get* spec :node->container)] #(get % nc))
        container->parent (let [cp (get* spec :container->parent)] #(get cp %))
        container->attrs (if-let [ca (get* spec :container->attrs)] #(get ca %)
                                 (if (and (get* spec :container-template)
                                          (get* spec :container->data))
                                   (let [ct (get* spec :container-template)
                                         cd (get* spec :container->data)]
                                     (comp (spec-fn ct) #(get* cd %)))
                                   (constantly nil)))
        directives (get* spec :directives)
        template (get* spec :template)
        dictim-fn-params (cond->
                             {:node->key node->key
                              :node->attrs node-fn
                              :edge->attrs edge-fn
                              :cluster->attrs container->attrs
                              :edge->src-key #(or (get % "src") (get % :src))
                              :edge->dest-key #(or (get % "dest") (get % :dest))}
                             node->container (assoc :node->cluster node->container)
                             container->parent (assoc :cluster->parent container->parent))
        dictim (g/graph->dictim nodes edges dictim-fn-params)]
    (if template
      (tm/apply-template dictim {:template template :directives directives :merge? true :new-priority? false})
      (if directives (cons directives dictim) dictim))))
