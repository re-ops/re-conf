(ns shim.cljs.shell
  (:require-macros  [cljs.core.async.macros :refer  [go]])
  (:require 
    [cljs.nodejs :as nodejs]
    [cljs.core.async :as a :refer [put! <! chan alts! timeout]]))

(def spawn (.-spawn (js/require "child_process")))

(defn exec-chan
  "spawns a child process for cmd with args. routes stdout, stderr, and
  the exit code to a channel. returns the channel immediately."
  [cmd args]
  (let [c (chan), p (spawn cmd args)]
    (.on (.-stdout p) "data"  #(put! c [:out  (str %)]))
    (.on (.-stderr p) "data"  #(put! c [:err  (str %)]))
    (.on p            "close" #(put! c [:exit (str %)]))
    c))

(defn exec
  "executes cmd with args. returns a channel immediately which
  will eventually receive a result vector of pairs [:kind data-str]
  with the last pair being [:exit code]"
  [cmd & args]
  (let [c (exec-chan cmd (clj->js args))]
    (go   (loop [output (<! c)               , result []]
    ;;(go (loop [output (alts! c (timeout 2)), result []]
            (if (= :exit (first output))
              (conj result output)
              (recur (<! c) (conj result output)))))))

(defn sh [& args]
  (go
    (let [result (into {} (<! (apply exec args)))]
       (println result))))
 

(comment 
  (sh "ls" "-la" "/home/ronen")
  )
