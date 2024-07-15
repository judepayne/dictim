(ns dictim.template-test
  (:require [clojure.test :refer :all]
            [dictim.template :as t]
            [dictim.tests :as tests]))


(def edn
  '(["Process View"
     ["p113"
      ["app14149" "Solar Wind"]
      ["app14027" "Leventine Sky"]
      [:comment "Christine's domain"]]
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
      ["app14027" "Leventine Sky" {:class "lemony"}]
      [:comment "Christine's domain"]]
     ["p114"
      ["app14181" "eBed" {:class "lemony"}]
      ["app14029" "Storm" {:style {:fill "'#f7f6f5'"}}]]
     ["p113" "->" "p114" "various flows"]]))


(def decorated-edn2
  '(["Process View"
     ["p113"
      ["app14149" "Solar Wind" {:class "lemony"}]
      ["app14027" "Leventine Sky"]
      [:comment "Christine's domain"]]
     ["p114" ["app14181" "eBed"] ["app14029" "Storm" {:class "lemony"}]]
     ["p113" "->" "p114" "various flows"]]))


(def dirs2
  '({:random 1}
    {:direction :right
     :classes
     {"lemony"
      {:style
       {:fill "'#ebe96e'"
        :border-radius 5}}}}))


(def decorated-edn3
  '({:random 1}
    {:direction :right,
     :classes {"lemony" {:style {:fill "'#ebe96e'", :border-radius 5}}}}
    ["Process View"
     ["p113"
      ["app14149" "Solar Wind" {:style {:fill "'#f7f6f5'"}}]
      ["app14027"
       "Leventine Sky"
       {:style {:fill "'#f7f6f5'"}, :class "lemony"}]
      [:comment "Christine's domain"]]
     ["p114"
      ["app14181" "eBed" {:style {:fill "'#f7f6f5'"}, :class "lemony"}]
      ["app14029" "Storm" {:style {:fill "'#f7f6f5'"}}]]
     ["p113" "->" "p114" "various flows"]]))


(deftest apply-template
  (testing "I can override styles and directives to a piece of dictim"
    (is (= (t/apply-template edn {:template template :directives dirs})
           decorated-edn)
        (= (t/apply-template edn {:template template2})
           decorated-edn2)))
  (testing "I can merge styles and directives to a piece of dictim"
    (is (= (t/apply-template edn {:template template :directives dirs2 :all-matching-clauses? true})
             decorated-edn3))))


(deftest removing-styles
  (testing "I can remove all styles"
    (is (= (t/remove-attrs decorated-edn)
           edn))))


(deftest directives
  (testing "directives are preserved"
    (is (= (t/apply-template
            '({:direction :right} [:ashape "A Shape"])
            {:template '(["=" "label" "A Shape"] {:style.fill "blue"}) :merge? true})
           '({:direction :right} [:ashape "A Shape" {:style.fill "blue"}])))))


(deftest vars
  (testing "vars are preserved when specified in remove-styles"
    (is (= (t/remove-attrs '({:direction :right} {"vars" {:a 2}} [:ashape "A Shape"])
                            :retain-vars? true)
           '({"vars" {:a 2}} [:ashape "A Shape"])))))


;; accessors tests
(def ctr [:aShape "Shape" {:style.fill "red"} [:bShape]])
(def conn [:x "->" "y" "--" :zoo "conn label"])


(deftest accessors
  (testing "Accessors can be used to extract values from elements"
    (is (true? (tests/test-elem ["=" "key" :aShape] ctr)))
    (is (true? (tests/test-elem ["!=" "key" :bShape] ctr)))
    (is (true? (tests/test-elem ["=" "label" "Shape"] ctr)))
    (is (true? (tests/test-elem ["!=" "label" "Sh."] ctr)))
    (is (true? (tests/test-elem ["=" "attrs" {:style.fill "red"}] ctr)))
    (is (true? (tests/test-elem ["=" "children" '([:bShape])] ctr)))
    (is (true? (tests/test-elem ["=" "keys" '(:x "y" :zoo)] conn)))
    (is (true? (tests/test-elem ["=" "label" "conn label"] conn)))))


;; setter tests

(deftest setters
  (testing "Setters can set attrs"
    (is (= (tests/set-attrs! [:one] {:style.fill "red"}) [:one {:style.fill "red"}]))
    (is (= (tests/set-attrs! [:one "lbl"] {:style.fill "red"}) [:one "lbl" {:style.fill "red"}]))
    (is (= (tests/set-attrs! [:one [1 2]] {:style.fill "red"}) [:one {:style.fill "red"} [1 2]]))
    (is (= (tests/set-attrs! [:one "lbl" {:style.fill "blue"}[1 2]] {:style.fill "red"})
           [:one "lbl" {:style.fill "red"} [1 2]]))
    (is (= (tests/set-attrs! [:x "->" :y] {:style.fill "red"}) [:x "->" :y {:style.fill "red"}]))
    (is (= (tests/set-attrs! [:x "->" :y "<-" :z] {:style.fill "red"}) [:x "->" :y "<-" :z {:style.fill "red"}]))
    (is (= (tests/set-attrs! [:x "->" :y "<-" :z "lbl"] {:style.fill "red"})
           [:x "->" :y "<-" :z "lbl" {:style.fill "red"}]))
    (is (= (tests/set-attrs!
            [:x "->" :z [0] {:style.fill "blue"}] {:style.fill "red"})
           [:x "->" :z [0] {:style.fill "red"}])))
  (testing "Setters can set labels"
    (is (= (tests/set-label! [:one] "lbl") [:one "lbl"]))
    (is (= (tests/set-label! [:one "lblz" {:style.fill "blue"}] "lbl") [:one "lbl" {:style.fill "blue"}]))
    (is (= (tests/set-label! [:one "lblz" {:style.fill "blue"} [1 2]] "lbl")
           [:one "lbl" {:style.fill "blue"} [1 2]]))
    (is (= (tests/set-label! [:x "->" :z [0] {:style.fill "blue"}] "lbl")
           [:x "->" :z [0] {:style.fill "blue"}]))
    (is (= (tests/set-label! [:x "->" :z "edge" {:style.fill "blue"}] "lbl")
           [:x "->" :z "lbl" {:style.fill "blue"}]))))


(def m
    {:a "sky"
     :b [1 2 3]
     :c 4})


(deftest test-elem-is-map
  (testing "Tests can run on maps rather than dictim elements"
    (is (true? (tests/test-elem ["=" :a "sky"] m)))
    (is (true? (tests/test-elem ["contains" :b 2] m)))
    (is (true? (tests/test-elem [">" :c 3] m)))))
