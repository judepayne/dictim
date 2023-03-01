(ns dictim.dot.compile-test
  (:require [clojure.test :refer :all]
            [dictim.dot.compile :as co]))

;; compilation

(def ex-graph
  '({:hello "you"}
    [:jude "Friends"
     [:tristram "T Biggs expanded"
      [:tris "TAB"]
      [:maddie "Madeline"]
      [:tris "--" :maddie "wedding bells?"]
      [:tris "->" :children "previously sired"]
      [:children "the brood"
       ["Oliver" "Eldest" {:style {:fill "orange"}}]
       ["Roof" "Good footballer" {:shape "person"}]]]]))
