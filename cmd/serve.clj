(ns serve
  (:require [org.httpkit.server :refer [run-server as-channel send!]]
            [hiccup.core :refer [html]]
            [babashka.pods :as pods]
            [clojure.java.browse :refer [browse-url]]))


(pods/load-pod 'org.babashka/fswatcher "0.0.3")
(require '[pod.babashka.fswatcher :as fw])


;; default port to serve from.
(def ^:dynamic *port* 8080)


;; default ip to serve from.
(def ^:dynamic *ip* "localhost")


;; target is the file to watch
(def file (atom nil))


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
(defn- body [] (@transform (slurp @file)))


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


(defn- start-watch [tgt]
  (reset! watcher
          (fw/watch tgt
                    (fn [event]
                      (when (or (= :write (:type event)) (= writechmod (:type event)))
                        (notify-clients (body))
                        (println (now) "broadcasting update"))))))


(defn- stop-watch []
  (fw/unwatch @watcher)
  (reset! watcher nil))


(defn- set-tgt-file [tgt] (reset! file tgt))


(defn start [tgt transform-fn]
  (set-tgt-file tgt)
  (reset! transform transform-fn)
  (start-watch tgt)
  (start-server)
  nil)

;; for development
(defn stop []
  (stop-watch)
  (stop-server)
  (println (str (now) " stopped serving.")))
