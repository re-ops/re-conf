(ns re-conf.resources.shell
  (:require-macros
   [clojure.core.strint :refer (<<)]
   [cljs.core.async.macros :refer [go]])
  (:require
   [re-conf.resources.log :refer (channel? info debug error)]
   [cuerdas.core :as str]
   [cljs.nodejs :as nodejs]
   [re-conf.resources.common :refer (run)]
   [cljs.core.match :refer-macros  [match]]
   [cljs.core.async :as async :refer [put! <! >! chan alts! timeout take!]]))

(def spawn (.-spawn (js/require "child_process")))

(def spawn-sync (.-spawnSync (js/require "child_process")))

(defn exec-chan
  "spawns a child process for cmd with args. routes stdout, stderr, and
  the exit code to a channel. returns the channel immediately."
  [cmd args opts]
  (let [c (chan) p (spawn cmd args opts)]
    (.on (.-stdout p) "data"  #(put! c [:out  (str %)]))
    (.on (.-stderr p) "data"  #(put! c [:err  (str %)]))
    (.on p "close" #(put! c [:exit (str %)]))
    (.on p "error" (fn [e] (put! c [:error e])))
    c))

(defn- execute
  "Executes cmd with args. returns a channel immediately which
    will eventually receive a result vector of pairs [:kind data-str]
    with the last pair being [:exit code]"
  [cmd args opts]
  (let [c (exec-chan cmd (clj->js args) (clj->js opts))]
    (go
      (loop [output (<! c) result {}]
        (match output
          [:exit code] (assoc result :exit (str/parse-int code))
          [:error e] {:error e}
          :else
          (recur (<! c) (update result (first output) str (second output))))))))

(defn opts-split [as]
  (let [[args opts] (split-with string? as)]
    [args (apply hash-map opts)]))

(defn apply-options [args opts]
  (let []
    (cond->> args
      (opts :sudo) (into ["/usr/bin/sudo"])
      (opts :dry) (or ["echo" "'dry run!'"]))))

(defn- sh [& as]
  (go
    (let [[cmdline opts] (opts-split as)
          [cmd & args] (apply-options cmdline opts)]
      (try
        (let [{:keys [exit] :as r} (into {} (<! (execute cmd args opts)))]
          (if (= 0 exit)
            {:ok r}
            {:error r}))
        (catch js/Error e
          {:error e})))))

(defn exec
  "Shell execution resource"
  [a & args]
  (if (channel? a)
    (run a #(apply sh args))
    (run nil #(apply sh (conj args a)))))

(defn exec-sync
  "Sync exec (not resource)"
  [cmd & as]
  (let [[args opts] (opts-split as)]
    (js->clj
     (spawn-sync cmd (clj->js args) (clj->js opts)))))

(defn unless
  "Run shell only if c returns :ok"
  [c & args]
  (go
    (let [{:keys [ok error] :as m} (<! c)]
      (if-not ok
        (<! (apply exec args))
        {:ok (<< "skipping due to ~{error}")}))))

(comment
  (info (sh "ls" "/foo" :sudo true) ::take))
