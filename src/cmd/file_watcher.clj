(ns cmd.file-watcher
  (:require [clojure.core.async :as async :refer [chan go-loop <! alt! timeout thread close!]]
            [babashka.fs :as fs])
  (:refer-clojure :exclude [add-watch]))


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
  [channel]
  (close! channel))


;; state
(def ^:private active-watches (atom nil))

(defn- set-add-watch! [path modified watch-chan]
  (swap! active-watches
         assoc
         path
         {:modified modified :watch-chan watch-chan}))

(defn- set-stop-watching! [path]
  (swap! active-watches dissoc path))


(defn- set-last-modified! [path modified]
  (swap! active-watches assoc-in [path :modified] modified))


(defn- path-last-modified [path]
  (:modified (get @active-watches path)))


(defn- path-channel [path]
  (:watch-chan (get @active-watches path)))
;;


(defn- file-exists?
  [path]
  (if (fs/exists? path)
    true
    (throw (Exception. "The file does not exist"))))


(defn- last-modified
  [path]
  (:lastModifiedTime (fs/read-attributes path "lastModifiedTime")))


(defn- watch-fn [path f]
  (fn []
    (let [m (last-modified path)]
      (when (not= m (path-last-modified path))
        (f)
        (set-last-modified! path m)))))


(defn add-watch
  "Watches the file at path and calls f each time it's modified"
  [path f]
  (when (file-exists? path)
    (set-add-watch!
     path
     (last-modified path)
     (periodically
      (watch-fn path f)
      300))))


(defn stop-watching
  "Stop watching the file at path"
  [path]
  (stop-periodically (path-channel path))
  (set-stop-watching! path))
