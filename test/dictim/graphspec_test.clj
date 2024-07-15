(ns dictim.graphspec-test
  (:require [clojure.test :refer :all]
            [dictim.graphspec :as g]))


(def spec1
  {"nodes"
   [{"id" "app12872",
     "name" "Trade pad",
     "owner" "Lakshmi",
     "dept" "Finance",
     "functions" ["Position Keeping" "Quoting"],
     "tco" 1200000,
     "process" "p.112"}
    {"id" "app12873",
     "name" "Data Source",
     "owner" "India",
     "dept" "Securities",
     "functions" ["Booking" "Order Mgt"],
     "tco" 1100000,
     "process" "p.114"}
    {"id" "app12874",
     "name" "Crypto Bot",
     "owner" "Joesph",
     "dept" "Equities",
     "functions" ["Accounting" "Booking"],
     "tco" 500000,
     "process" "p.112"}
    {"id" "app12875",
     "name" "Data Solar",
     "owner" "Deepak",
     "dept" "Securities",
     "functions" ["Position Keeping" "Data Master"],
     "tco" 1000000,
     "process" "p.114"}
    {"id" "app12876",
     "name" "Data Solar",
     "owner" "Lakshmi",
     "dept" "Risk",
     "functions" ["Accounting" "Data Master"],
     "tco" 1700000,
     "process" "p.114"}],
   "edges"
   [{"src" "app12874",
     "dest" "app12875",
     "data-type" "security reference"}
    {"src" "app12874", "dest" "app12876", "data-type" "quotes"}
    {"src" "app12875", "dest" "app12875", "data-type" "instructions"}
    {"src" "app12874", "dest" "app12872", "data-type" "instructions"}
    {"src" "app12875", "dest" "app12874", "data-type" "client master"}
    {"src" "app12875", "dest" "app12874", "data-type" "allocations"}],
   "node->key" "id",
   "node->container" "dept",
   "container->parent"
   {"Finance" "2LOD",
    "Risk" "2LOD",
    "Securities" "FO",
    "Equities" "FO"},
   "node-template"
   [["=" "dept" "Equities"] {"label" ["This dept is %s" "dept"], "style.fill" "blue"}
    "else" {"label" ["%s" "dept"]}],
   "edge-template" ["else" {"label" ["data type: %s" "data-type"]}],
  ; "container->attrs" {"Securities" {"style.fill" "green"}}
   "container->data" {"Securities" {"head" "Amit Singh"
                                    "revenue $Bn" 1.1}
                      "Equities" {"head" "Peter Nevitt"
                                  "revenue $Bn" 0.55}
                      "Risk" {"head" "Amineer Singh"
                              "revenue $Bn" 0}
                      "Finance" {"head" "Cynthia Parcelle"
                                 "revenue $Bn" 0}
                      "2LOD" {"head" "Markus Bauer"
                              "revenue $Bn" 0}
                      "FO" {"head" "Mia Fischer"
                            "revenue $Bn" 2.37}}
   "container-template" [[">" "revenue $Bn" 1] {"style.fill" "'#c47321'"}
                         [">" "revenue $Bn" 0.5] {"style.fill" "'#ebb178'"}]})


(def dict1
  '(["2LOD"
     ["Finance" ["app12872" "Finance"]]
     ["Risk" ["app12876" "Risk"]]]
    ["FO"
     {"style.fill" "'#c47321'"}
     ["Securities"
      {"style.fill" "'#c47321'"}
      ["app12873" "Securities"]
      ["app12875" "Securities"]]
     ["Equities"
      {"style.fill" "'#ebb178'"}
      ["app12874" "This dept is Equities" {"style.fill" "blue"}]]]
    ["FO.Equities.app12874"
     "->"
     "FO.Securities.app12875"
     "data type: security reference"]
    ["FO.Equities.app12874"
     "->"
     "2LOD.Risk.app12876"
     "data type: quotes"]
    ["FO.Securities.app12875"
     "->"
     "FO.Securities.app12875"
     "data type: instructions"]
    ["FO.Equities.app12874"
     "->"
     "2LOD.Finance.app12872"
     "data type: instructions"]
    ["FO.Securities.app12875"
     "->"
     "FO.Equities.app12874"
     "data type: client master"]
    ["FO.Securities.app12875"
     "->"
     "FO.Equities.app12874"
     "data type: allocations"]))


(deftest non-keyword-graphspec
  (testing "I can produce dictim"
    (is (= (g/graph-spec->dictim spec1)
           dict1))))


(def spec2
  {:node->key :id,
   :container->data
   {"Securities" {:head "Amit Singh", :revenue 1.1},
    "Equities" {:head "Peter Nevitt", :revenue 0.55},
    "Risk" {:head "Amineer Singh", :revenue 0},
    "Finance" {:head "Cynthia Parcelle", :revenue 0},
    "2LOD" {:head "Markus Bauer", :revenue 0},
    "FO" {:head "Mia Fischer", :revenue 2.37}},
   :nodes
   [{:id "app12872",
     :name "Trade pad",
     :owner "Lakshmi",
     :dept "Finance",
     :functions ["Position Keeping" "Quoting"],
     :tco 1200000,
     :process "p.112"}
    {:id "app12873",
     :name "Data Source",
     :owner "India",
     :dept "Securities",
     :functions ["Booking" "Order Mgt"],
     :tco 1100000,
     :process "p.114"}
    {:id "app12874",
     :name "Crypto Bot",
     :owner "Joesph",
     :dept "Equities",
     :functions ["Accounting" "Booking"],
     :tco 500000,
     :process "p.112"}
    {:id "app12875",
     :name "Data Solar",
     :owner "Deepak",
     :dept "Securities",
     :functions ["Position Keeping" "Data Master"],
     :tco 1000000,
     :process "p.114"}
    {:id "app12876",
     :name "Data Solar",
     :owner "Lakshmi",
     :dept "Risk",
     :functions ["Accounting" "Data Master"],
     :tco 1700000,
     :process "p.114"}],
   :container->parent
   {"Finance" "2LOD", "Risk" "2LOD", "Securities" "FO", "Equities" "FO"},
   :edge-template ["else" {:label ["data type: %s" :data-type]}],
   :node-template
   [["=" :dept "Equities"]
    {:label ["This dept is %s" :dept], :style.fill "blue"}
    "else"
    {:label ["%s" :dept]}],
   :container-template
   [[">" :revenue 1]
    {:style.fill "'#c47321'"}
    [">" :revenue 0.5]
    {:style.fill "'#ebb178'"}],
   :node->container :dept,
   :edges
   [{:src "app12874",
     :dest "app12875",
     :data-type "security reference"}
    {:src "app12874", :dest "app12876", :data-type "quotes"}
    {:src "app12875", :dest "app12875", :data-type "instructions"}
    {:src "app12874", :dest "app12872", :data-type "instructions"}
    {:src "app12875", :dest "app12874", :data-type "client master"}
    {:src "app12875", :dest "app12874", :data-type "allocations"}]})


(def dict2
  '(["2LOD"
     ["Finance" ["app12872" "Finance"]]
     ["Risk" ["app12876" "Risk"]]]
    ["FO"
     {:style.fill "'#c47321'"}
     ["Securities"
      {:style.fill "'#c47321'"}
      ["app12873" "Securities"]
      ["app12875" "Securities"]]
     ["Equities"
      {:style.fill "'#ebb178'"}
      ["app12874" "This dept is Equities" {:style.fill "blue"}]]]
    ["FO.Equities.app12874"
     "->"
     "FO.Securities.app12875"
     "data type: security reference"]
    ["FO.Equities.app12874"
     "->"
     "2LOD.Risk.app12876"
     "data type: quotes"]
    ["FO.Securities.app12875"
     "->"
     "FO.Securities.app12875"
     "data type: instructions"]
    ["FO.Equities.app12874"
     "->"
     "2LOD.Finance.app12872"
     "data type: instructions"]
    ["FO.Securities.app12875"
     "->"
     "FO.Equities.app12874"
     "data type: client master"]
    ["FO.Securities.app12875"
     "->"
     "FO.Equities.app12874"
     "data type: allocations"]))


(deftest keyword-graphspec
  (testing "I can produce dictim"
    (is (= (g/graph-spec->dictim spec2)
           dict2))))


(def spec3
  {:node->key :id,
   :container->data
   {"Securities" {:head "Amit Singh", :revenue 1.1},
    "Equities" {:head "Peter Nevitt", :revenue 0.55},
    "Risk" {:head "Amineer Singh", :revenue 0},
    "Finance" {:head "Cynthia Parcelle", :revenue 0},
    "2LOD" {:head "Markus Bauer", :revenue 0},
    "FO" {:head "Mia Fischer", :revenue 2.37}},
   :nodes
   [{:id "app12872",
     :name "Trade pad",
     :owner "Lakshmi",
     :dept "Finance",
     :functions ["Position Keeping" "Quoting"],
     :tco 1200000,
     :process "p.112"}
    {:id "app12873",
     :name "Data Source",
     :owner "India",
     :dept "Securities",
     :functions ["Booking" "Order Mgt"],
     :tco 1100000,
     :process "p.114"}
    {:id "app12874",
     :name "Crypto Bot",
     :owner "Joesph",
     :dept "Equities",
     :functions ["Accounting" "Booking"],
     :tco 500000,
     :process "p.112"}
    {:id "app12875",
     :name "Data Solar",
     :owner "Deepak",
     :dept "Securities",
     :functions ["Position Keeping" "Data Master"],
     :tco 1000000,
     :process "p.114"}
    {:id "app12876",
     :name "Data Solar",
     :owner "Lakshmi",
     :dept "Risk",
     :functions ["Accounting" "Data Master"],
     :tco 1700000,
     :process "p.114"}],
   :container->parent
   {"Finance" "2LOD", "Risk" "2LOD", "Securities" "FO", "Equities" "FO"},
   :edge-template ["else" {:label ["data type: %s" :data-type]}],
   :node-template
   [["=" :dept "Equities"]
    {:label ["This dept is %s" :dept], :style.fill "blue"}
    "else"
    {:label ["%s" :dept]}],
   :container-template
   [[">" :revenue 1]
    {:style.fill "'#c47321'"}
    [">" :revenue 0.5]
    {:style.fill "'#ebb178'"}],
   :node->container :dept,
   :edges
   [{:src "app12874",
     :dest "app12875",
     :data-type "security reference"}
    {:src "app12874", :dest "app12876", :data-type "quotes"}
    {:src "app12875", :dest "app12875", :data-type "instructions"}
    {:src "app12874", :dest "app12872", :data-type "instructions"}
    {:src "app12875", :dest "app12874", :data-type "client master"}
    {:src "app12875", :dest "app12874", :data-type "allocations"}]
   :directives {:classes {:lemony {:style.fill "yellow"}}}
   :template '(["=" "element-type" "shape"] {:style.fill "yellow"})})


(def dict3
  '({:classes {:lemony {:style.fill "yellow"}}}
    ["FO.Securities.app12875"
     "->"
     "FO.Equities.app12874"
     "data type: allocations"]
    ["FO.Securities.app12875"
     "->"
     "FO.Equities.app12874"
     "data type: client master"]
    ["FO.Equities.app12874"
     "->"
     "2LOD.Finance.app12872"
     "data type: instructions"]
    ["FO.Securities.app12875"
     "->"
     "FO.Securities.app12875"
     "data type: instructions"]
    ["FO.Equities.app12874"
     "->"
     "2LOD.Risk.app12876"
     "data type: quotes"]
    ["FO.Equities.app12874"
     "->"
     "FO.Securities.app12875"
     "data type: security reference"]
    ["FO"
     ["Securities"
      ["app12873" "Securities" {:style.fill "yellow"}]
      {:style.fill "'#c47321'"}
      ["app12875" "Securities" {:style.fill "yellow"}]]
     {:style.fill "'#c47321'"}
     ["Equities"
      {:style.fill "'#ebb178'"}
      ["app12874" "This dept is Equities" {:style.fill "blue"}]]]
    ["2LOD"
     ["Finance" ["app12872" "Finance" {:style.fill "yellow"}]]
     ["Risk" ["app12876" "Risk" {:style.fill "yellow"}]]]))


(deftest directives-and-template
  (testing "directives are merged in. template is merged in but node-template, edge-template, container-template take precedence"
    (is (= (g/graph-spec->dictim spec3)
           dict3))))
