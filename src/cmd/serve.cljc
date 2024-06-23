;; .cljc extentsion to allow reader conditionals.
(ns cmd.serve
  (:require [org.httpkit.server :refer [run-server as-channel send!]]
            [hiccup.core :refer [html]]
            [cmd.file-watcher :as fw]
            [clojure.java.browse :refer [browse-url]]))


;; default port to serve from.
(def ^:dynamic *port* 8080)


;; default ip to serve from.
(def ^:dynamic *ip* "localhost")


;; a cached sequence of files' contents that are being watched
(def files (atom [])) 


(defn- cache-files [paths]
  (mapv
   (fn [path] (swap! files conj [path (slurp path)]))
   paths))


(defn- index-of-path [path cached-files]
  (reduce (fn [acc [cached-path cached-contents]]
            (if (= path cached-path)
              (reduced acc)
              (inc acc)))
          0
          cached-files))


(defn- recache-file [path]
  (let [index (index-of-path path @files)]
    (swap! files assoc index [path (slurp path)])))


(defn- cached-file-contents []
  (map second @files))


;; transform-fn to be applied to file's (new) contents
(def transform (atom nil))


;; client side script to listen on websocket and receive new page, + css
(defn- ws-head []
  (str
   "<style>
   #output {
     font-size: 30px;
   }
   </style>"
   "<script type=\"text/javascript\">
    // use vanilla JS because why not
    window.addEventListener(\"load\", function() {
        
        // create websocket instance
        var mySocket = new WebSocket(\"ws://" *ip*  ":" *port* "/ws\");
        
        // add event listener reacting when message is received
        mySocket.onmessage = function (event) {
           var output = document.getElementById(\"output\");
           output.innerHTML = event.data;
        };
    });
    </script>"))


;; --State--
;; clients (websocket channels.
(defonce clients (atom #{}))
;; holds the server - a function that shuts down the server.
(defonce server (atom nil))
;; the file watcher
(defonce watcher (atom nil))


;; server side websocket handler
(defn- ws-handler [req]
  (as-channel req
              {:on-open (fn [ch] (swap! clients conj ch))
               :on-close (fn [ch _] (swap! clients disj ch))}))


;; Our app

;; body updates the cache of files we are watching contents
;; and then applies the transform function over that cache.
(defn- body
  ([]
   (apply @transform (cached-file-contents)))
  ([path]
   (recache-file path)
   (apply @transform (cached-file-contents))))


(defn- app [req]
  (case (:uri req)
    "/"      {:status  200
              :headers {"Content-Type" "text/html"}
              :body    (html [:html
                              [:head
                               (ws-head)]
                              [:body
                               [:div {:id "output"}
                                (body)]]])}
    "/ws"    (ws-handler req)

    {:status 404
     :body "Not found."
     :headers {"Content-Type" "text/html"}}))


(defn- start-server []
  (reset! server (run-server #'app {:ip *ip* :port *port*}))
  (browse-url (str "http://" *ip* ":" *port*)))


(defn- stop-server []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)
    (reset! clients #{})))


(defn- notify-clients [msg]
  (doseq [cli @clients]
    (send! cli msg)))


(defn- now []
  (let [date (java.time.LocalDateTime/now)
        formatter (java.time.format.DateTimeFormatter/ofPattern "HH:mm:ss")]
    (str "[" (.format date formatter) "]")))


(def ^:private writechmod (keyword "write|chmod"))

;; TODO add a watch for each file
(defn- start-watch [paths]
  (mapv
   (fn [path]
     (fw/add-watch path
                   #(do (notify-clients (body path))
                        (println (now) "broadcasting update"))))
   paths))


(defn- stop-watch [path]
  (fw/stop-watching path))


(defn start [transform-fn & paths]
  (cache-files paths)
  (reset! transform transform-fn)
  (start-watch paths)
  (start-server)
  nil)

;; for development
(defn stop [& paths]
  (map fw/stop-watching paths)
  (reset! files [])
  (stop-server)
  (println (str (now) " stopped serving.")))
