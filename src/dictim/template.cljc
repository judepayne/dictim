(ns
    ^{:author "judepayne"
      :doc "Namespace for handling dictim templates"}
    dictim.template
  (:require [dictim.utils :as utils :refer [elem-type elements?]]
            [clojure.string :as str]
            [dictim.tests :as tests])
  (:refer-clojure :exclude [key keys test]))


(defn- principal-elem? [form]
  (contains? #{:shape :ctr :conn :conn-ref} (elem-type form)))


(defn walk-dictim
  "Similar to clojure.walk/walk but refined for dictim. Only walks into
   children of elements."
  [inner outer element]
  (case (elem-type element)
    :elements (outer (apply list (map inner element)))
    :ctr (let [[nc c] (split-with (complement vector?) element)]
           (outer (apply conj (into [] nc)
                         (mapv inner c))))
    (outer element)))


(defn postwalk-dictim
  "Similar to clojure.walk/postwalk but for dictim."
  [f element]
  (walk-dictim (partial postwalk-dictim f) f element))


(defn prewalk-dictim
  "Similar to clojure.walk/prewalk but for dictim."
  [f element]
  (walk-dictim (partial prewalk-dictim f) identity (f element)))


(defn deep-merge [v & vs]
  (letfn [(rec-merge [v1 v2]
            (if (and (map? v1) (map? v2))
              (merge-with deep-merge v1 v2)
              v2))]
    (if (some identity vs)
      (reduce #(rec-merge %1 %2) v vs)
      v)))


(defn- prep-directives
  ([new] (prep-directives nil new))
  ([old new]
   (reduce
    (fn [acc [k v]]
      (conj acc {k v}))
    []
    (deep-merge old new))))


(defn apply-template
  "Walks the supplied dictim edn and decorates with attrs and top level directives.
   A template is map with the key `:template` and optionally `:directives`, `:merge?`, 
   `:new-priority?` and `:all-matching-clauses`
   The value under the `:template` key can be either:
     - a function that takes an elem and returns the attrs to be added
       to the elem (or nil).
     - a sequence of test and attribute maps  pairs to be applied if the test succeeds.
     A comparison test has the form [<comparator> <accessor> <value>] where:
       - <comparator> is a string, one of 'matches' '=' '!=' 'contains' 'doesnt-contain'
         '>' '<' '<=' or '>='
       - <accessor> is a string, one of 'key' 'label' 'attrs' 'keys' 'children'
         or 'element-type'. Accessors allow you to access values of a dictim element,
         for example its `key` or `label`.
       - <value> is the value to be tested against.
         Example simple test: `[\"=\" \"key\" \"node123\"]`
       A regex matching test has the form [\"matches\" <accessor> <regex-string>] where:
       - <accessor> .. same as for comparison test
       - <regex-string> .. a string representation of a java.util.regex.Pattern
       A nested test nests comparison/ matching tests with `and` and `or` statements.
         Example nested test:
         `[\"and\" [\"=\" \"type\" \"ctr\"] [\"=\" \"key\" \"node123\"]]`
       Attributes are supplied using standard dictim, e.g. `{:style.fill \"red\"}`
   `:merge?` If false, new attributes overwrite the original.
    If true, then if `:new-priority?` is true, new attributes have priority over
    old when merged, otherwise old attributes have priority in the merge. 
   directives is a map of attrs to be added at the top level e.g. `{\"classes\"...}`
   If there are directives in the original dict, the new directives will be merge over them
   if merge? is true, otherwise they will be overwritten."
  [dict {:keys [template directives merge? new-priority? all-matching-clauses?]
           :or {merge? false new-priority? true all-matching-clauses? false}}]
  (let [attrs-fn (if (fn? template)
                   template
                   (if all-matching-clauses? (tests/test-fn-merge template) (tests/test-fn template)))
        edit-fn (fn [form]
                  (if (principal-elem? form)
                    (let [new-attrs (attrs-fn form)
                          attrs (if merge?
                                  (if new-priority?
                                    (deep-merge (tests/attrs form) new-attrs)
                                    (deep-merge new-attrs (tests/attrs form)))
                                  new-attrs)]
                      (if attrs
                        (tests/set-attrs! form attrs)
                        form))
                    form))
        old-dirs (reduce merge (filter map? dict))
        data-elements (into '() (remove map? dict))
        new-dirs (cond
                   (and merge? (seq? directives))
                   (prep-directives old-dirs (reduce merge directives))

                   (seq? directives)         directives

                   merge?                    (prep-directives old-dirs directives)

                   :else                     (prep-directives directives))
        walked (postwalk-dictim edit-fn data-elements)]
    (if new-dirs
      (if (list? walked)
        (concat new-dirs walked)
        (concat new-dirs (list walked)))
      walked)))


(defn apply-templates
  "Merges multiple templates into the supplied dictim, with merge? true."
  [dict & templates]
  (reduce
   (fn [acc cur]
     (apply-template acc cur true))
   dict
   templates))


(defn remove-attrs
  "removes all maps from the nested form. i.e. attrs and directives.
   If retain-vars? is true, :vars/\"vars\" attributes will be retained in the dictim,
   since vars can be part of the 'data side' of a piece of dictim."
  [dict & {:keys [retain-vars?] :or {retain-vars? false}}]
  (let [remove-fn (fn [form]
                    (cond
                      (map? form)           (if retain-vars?
                                              (let [vars (select-keys form [:vars "vars"])]
                                                (if (empty? vars)
                                                  nil
                                                  vars))
                                              nil)

                      (principal-elem? form)
                      (into [] (remove map? form))

                      :else form))]
    (remove nil? (prewalk-dictim remove-fn dict))))
