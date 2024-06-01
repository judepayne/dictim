(ns ^{:author "judepayne"
      :doc "Namespace for formatting d2 output of dictim."}
    dictim.format
  (:require [clojure.string :as str]))


;; atom to hold current number of spaces to indent at beginning of line
(def ^:private indentation-counter (atom 0))


;; atom to hold the current indentation step
(def ^:private tab-value (atom 0))


;; atom to determine if whether inside a var or not
(def ^:private in-var? (atom false))


;; indent: i.e. increase indentation counter by tab-value
(defn- ind! [] (swap! indentation-counter #(+ @tab-value %)) nil)


;; outdent: i.e. decrease indentation counter by tab-value
(defn- outd! [] (swap! indentation-counter #(- % @tab-value)) nil)


;; returns an empty substring with length of counter. pads new lines
(defn- tabs [] (apply str (repeat @indentation-counter \space)))


(defn- trim-lines
  [s]
  (->>
   (str/split s #"\n")
   (map str/trim)
   (interpose "\n")
   str/join))


(defn- remove-n [s n]
  (let [c (count s)]
    (.substring s 0 (- c n))))


(defn fmt
  "Formats a d2 string. :tab is the width of one indentation step."
  [d2s & {:keys [tab]
          :or {tab 2}}]
  (reset! indentation-counter 0)
  (reset! tab-value tab)
  (reduce
   (fn [acc cur]
     (case cur
       \{        (if (= (last acc) \$)
                   (do (reset! in-var? true) (str acc cur))
                   (do (ind!) (str acc \space \{)))
       
       \}        (if @in-var?
                   (do (reset! in-var? false) (str acc cur))
                   (do (outd!) (str (remove-n acc @tab-value) \})))
       
       \newline  (str acc \newline (tabs))
       
       (str acc cur)))
   nil
   (-> d2s trim-lines)))


(defn fmt-json
  "Formats a json string. :tab is the width of one indentation step."
  [jss & {:keys [tab]
          :or {tab 2}}]
  (reset! indentation-counter 0)
  (reset! tab-value tab)
  (reduce
   (fn [acc cur]
     (case cur
       \[      (str acc \[ \newline (ind!) (tabs))
       \]      (str acc \newline (outd!) (tabs) \])
       \{      (str acc \{ \newline (ind!) (tabs))
       \}      (str acc \newline (outd!) (tabs) \})
       \,      (str acc \, \newline (tabs))

       (str acc cur)))
   nil
   jss))
