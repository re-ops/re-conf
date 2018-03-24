(ns shim.cljs.shell
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.nodejs :as nodejs]
    [cljs.core.async :as async :refer [put! <! chan alts! timeout take!]]))

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
  "Executes cmd with args. returns a channel immediately which
    will eventually receive a result vector of pairs [:kind data-str]
    with the last pair being [:exit code]"
  [cmd args]
  (let [c (exec-chan cmd (clj->js args))]
    (go
      (loop [output (<! c) result {}]
        (if (= :exit (first output))
          (update result :exit conj (second output))
          (recur (<! c) (update result (first output) str (second output))))))))

(defn apply-options [as]
  (let [[args options] (split-with string? as)]
    (if ((apply hash-map options) :sudo)
     (into ["/usr/bin/sudo"] args)
      args
      )))

(defn sh [& as]
  (go
    (let [[cmd & args] (apply-options as)]
      (into {}
        (<! (exec cmd args))))))

(comment
  (take! (sh "apt" "update") (fn [r] (println r)))
  )
