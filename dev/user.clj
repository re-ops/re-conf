(ns user
 (:require
   [cemerick.piggieback :as piggie]
   [cljs.repl.node :as node]
   [clojure.java.io :as io]
   [figwheel-sidecar.repl-api :as f]))

(defn fig-start
  []
  (f/start-figwheel!))

(defn fig-stop
  []
  (f/stop-figwheel!))

(defn cljs-repl
  []
  (f/cljs-repl))

(defn piggy []
   (piggie/cljs-repl
     (node/repl-env :host "192.168.122.161")
       :node-command "remote.sh"
       :output-dir "/tmp/cljs-repl-share"
       ))
