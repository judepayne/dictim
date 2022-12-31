(ns dictim.format
  (:require [clojure.string :as str]))


(def ^:private indentation-counter (atom 0))

(def ^:private tab-value (atom 0))

(defn- ind! [] (swap! indentation-counter #(+ @tab-value %)) nil)

(defn- outd! [] (swap! indentation-counter #(- % @tab-value)) nil)

(defn- tabs [] (apply str (repeat @indentation-counter \space)))


(defn- trim-lines
  [s]
  (->>
   (str/split s #"\n")
   (map str/trim)
   (interpose "\n")
   str/join))


(defn- prep
  [s]
  (-> s
      (str/replace #"; " ";")
      (str/replace #";" "\n")
      (str/replace #"[ ]{2,}" " ")
      (str/replace #"[\n]{2,}" "\n")
      (str/replace #"\{\n" "{")
      (str/replace #"\n\}" "}")
      (str/replace #": " ":")
      (str/replace #":" ": ")
      (str/replace #"(\s)+\{" "{")))


(defn fmt
  "Formats a d2 string. :tab is the width of one indentation step."
  [d2s & {:keys [tab]
          :or {tab 2}}]
  (reset! indentation-counter 0)
  (reset! tab-value tab)
  (reduce
   (fn [acc cur]
     (case cur
       \{        (do (ind!) (str acc
                                 \space
                                 \{ \newline (tabs)))
       \}        (do (outd!) (str acc \newline (tabs) \}))
       \newline  (str acc \newline (tabs))
       (str acc cur)))
   nil
   (-> d2s trim-lines prep)))
