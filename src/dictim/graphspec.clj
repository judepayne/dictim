(ns dictim.graphspec
  (:require [dictim.graph.core :as g]
            [dictim.tests :as t]
            [dictim.utils :refer [error]]
            [dictim.template :as tm]
            [dictim.validate :as v]
            [dictim.spec :as spec])
  (:refer-clojure :exclude [comparator test]))

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
    (let [m ((t/test-fn-merge tests) elem)
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
       overridden by the application of :template. A template applied to the resultant
       dicitm is a very different beast to applied as a node-template or edge-template
       since it no longer has the full data descriptions in nodes and edges available
       to it. A template therefore might look like this:
       :template [[\"and\" [\"=\" \"element-type\" \"shape\"]
                           [\"matches\" \"label\" \"S.+\"]]
                 {:class \"lemony\"}]
       quite is quite different to the below :node-template example

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
  [spec & {:keys [validate? output-format] :or {validate? true output-format :d2}}]

  (when validate?
    (spec/validate-graphspec spec output-format))

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
      (tm/apply-template dictim {:template template :directives directives :merge? true :new-priority? false :all-matching-clauses? true})
      (if directives (cons directives dictim) dictim))))
