(ns dictim.template-test
  (:require [clojure.test :refer :all]
            [dictim.template :as t]))


(def edn
  '(["Process View"
     ["p113"
      ["app14149" "Solar Wind"]
      ["app14027" "Leventine Sky"]]
     ["p114"
      ["app14181" "eBed"]
      ["app14029" "Storm"]]
     ["p113" "->" "p114" "various flows"]]))


(def template
  '(["and" ["=" "element-type" "shape"]
     ["or" ["=" "key" "app14181"] ["=" "key" "app14027"]]] {:class "lemony"}
    ["and" ["=" "element-type" "shape"]] {:style {:fill "'#f7f6f5'"}}))


(def template2
  '(["and" ["=" "element-type" "shape"] ["matches" "label" "S.+"]] {:class "lemony"}))


(def dirs
  {:direction :right
   :classes
   {"lemony"
    {:style
     {:fill "'#ebe96e'"
      :border-radius 5}}}})


(def decorated-edn
  '({:direction :right}
    {:classes {"lemony" {:style {:fill "'#ebe96e'", :border-radius 5}}}}
    ["Process View"
     ["p113"
      ["app14149" "Solar Wind" {:style {:fill "'#f7f6f5'"}}]
      ["app14027" "Leventine Sky" {:class "lemony"}]]
     ["p114"
      ["app14181" "eBed" {:class "lemony"}]
      ["app14029" "Storm" {:style {:fill "'#f7f6f5'"}}]]
     ["p113" "->" "p114" "various flows"]]))


(def decorated-edn2
  '(["Process View"
     ["p113"
      ["app14149" "Solar Wind" {:class "lemony"}]
      ["app14027" "Leventine Sky"]]
     ["p114" ["app14181" "eBed"] ["app14029" "Storm" {:class "lemony"}]]
     ["p113" "->" "p114" "various flows"]]))


(deftest adding-styles
  (testing "I can add styles and directives to a piece of dictim"
    (is (= (t/add-styles edn template dirs)
           decorated-edn)
        (= (t/add-styles edn template2)
           decorated-edn2))))


(deftest removing-styles
  (testing "I can remove all styles"
    (is (= (t/remove-styles decorated-edn)
           edn))))


(deftest directives
  (testing "directives are preserved"
    (is (= (t/add-styles '({:direction :right} [:ashape "A Shape"])
                         '(["=" "label" "A Shape"] {:style.fill "blue"}))
           '({:direction :right} [:ashape "A Shape" {:style.fill "blue"}])))))


(deftest vars
  (testing "vars are preserved when specified in remove-styles"
    (is (= (t/remove-styles '({:direction :right} {"vars" {:a 2}} [:ashape "A Shape"])
                            :retain-vars? true)
           '({"vars" {:a 2}} [:ashape "A Shape"])))))


;; accessors tests

(deftest accessors
  (testing "Accessors can be used to extract values from elements"
    (binding [t/*elem* [:aShape "Shape" {:style.fill "red"} [:bShape]]]
      (is (true? (t/test ["=" "key" :aShape])))
      (is (true? (t/test ["!=" "key" :bShape])))
      (is (true? (t/test ["=" "label" "Shape"])))
      (is (true? (t/test ["!=" "label" "Sh."])))
      (is (true? (t/test ["=" "attrs" {:style.fill "red"}])))
      (is (true? (t/test ["=" "children" '([:bShape])]))))
    (binding [t/*elem* [:x "->" "y" "--" :zoo "conn label"]]
      (is (true? (t/test ["=" "keys" '(:x "y" :zoo)])))
      (is (true? (t/test ["=" "label" "conn label"]))))))


;; testing regex matching tests

