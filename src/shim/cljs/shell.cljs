(ns shim.cljs.shell
  (:require-macros  [cljs.core.async.macros :refer  [go]])
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
  "executes cmd with args. returns a channel immediately which
  will eventually receive a result vector of pairs [:kind data-str]
  with the last pair being [:exit code]"
  [cmd & args]
  (let [c (exec-chan cmd (clj->js args))]
    (go (loop [output (<! c) result []]
      (if (= :exit (first output))
        (conj result output)
        (recur (<! c) (conj result output)))))))

(defn sh [& args]
  (go
    (into {} (<! (apply exec args)))))

(comment
  (take! (sh "ls" "-la" "/home/ronen") (fn [r] (println r)))
  (take!  (fn [r] (println (keys r)))))
