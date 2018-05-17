(ns re-conf.cljs.shell
  (:require-macros
   [clojure.core.strint :refer (<<)]
   [cljs.core.async.macros :refer [go]])
  (:require
   [re-conf.cljs.log :refer (channel? info debug error)]
   [cuerdas.core :as str]
   [cljs.nodejs :as nodejs]
   [re-conf.cljs.common :refer (run)]
   [cljs.core.match :refer-macros  [match]]
   [cljs.core.async :as async :refer [put! <! chan alts! timeout take!]]))

(def spawn (.-spawn (js/require "child_process")))

(defn exec-chan
  "spawns a child process for cmd with args. routes stdout, stderr, and
  the exit code to a channel. returns the channel immediately."
  [cmd args]
  (let [c (chan) p (spawn cmd args)]
    (.on (.-stdout p) "data"  #(put! c [:out  (str %)]))
    (.on (.-stderr p) "data"  #(put! c [:err  (str %)]))
    (.on p "close" #(put! c [:exit (str %)]))
    (.on p "error" (fn [e] (put! c [:error e])))
    c))

(defn- execute
  "Executes cmd with args. returns a channel immediately which
    will eventually receive a result vector of pairs [:kind data-str]
    with the last pair being [:exit code]"
  [cmd args]
  (let [c (exec-chan cmd (clj->js args))]
    (go
      (loop [output (<! c) result {}]
        (match output
          [:exit code] (assoc result :exit (str/parse-int code))
          [:error e] {:error e}
          :else
          (recur (<! c) (update result (first output) str (second output))))))))

(defn apply-options [as]
  (let [[args opts] (split-with string? as)
        options (apply hash-map opts)]
    (cond->> args
      (options :sudo) (into ["/usr/bin/sudo"])
      (options :dry) (or ["echo" "'dry run!'"]))))

(defn- sh [& as]
  (go
    (let [[cmd & args] (apply-options as)]
      (try
        (let [{:keys [exit] :as r} (into {} (<! (execute cmd args)))]
          (if (= 0 exit)
            {:ok r}
            {:error r}))
        (catch js/Error e
          {:error e})))))

(defn exec
  "Shell execution resource"
  [a & args]
  (if (channel? a)
    (run a  #(apply sh args))
    (run nil #(apply sh (conj args a)))))

(defn unless
  "Run shell only if f returns :ok"
  [c & args]
  (go
    (let [{:keys [ok error]} (<! c)]
      (if ok
        (<! (exec args))
        {:ok (<< "skipping due to ~{error}")}))))

(comment
  (info (sh "ls" "/foo" :sudo true) ::take))
