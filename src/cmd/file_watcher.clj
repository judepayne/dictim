(ns cmd.file-watcher
  (:require [clojure.core.async :as async :refer [chan go-loop <! alt! timeout thread close!]]
            [babashka.fs :as fs]))


(defn- periodically
  [f time-in-ms]
  (let [stop (chan)]
    (go-loop []
      (alt!
        (timeout time-in-ms) (do (<! (thread (f)))
                                 (recur))
        stop :stop))
    stop))


(defn- stop-periodically
  [loop]
  (close! loop))


(defn- file-exists?
  [path]
  (if (fs/exists? path)
    true
    (throw (Exception. "The file does not exist"))))


(def ^:private modified
  (atom nil))


(defn- last-modified
  [path]
  (:lastModifiedTime (fs/read-attributes path "lastModifiedTime")))


(defn- watch-fn [path f]
  (fn []
    (let [m (last-modified path)]
      (when (not= m (get @modified path))
        (f)
        (swap! modified assoc path m)))))


(def ^:private watch-chan (atom nil))


(defn watch
  "Watches the file at path and calls f each time it's modified"
  [path f]
  (when (file-exists? path)
    (swap! modified assoc path (last-modified path))
    (swap! watch-chan
           assoc path
           (periodically
            (watch-fn path f)
            300))))


(defn stop-watching
  "Stop watching the file at path"
  [path]
  (let [wc (get @watch-chan path)]
    (stop-periodically wc)
    (swap! watch-chan dissoc path)))
