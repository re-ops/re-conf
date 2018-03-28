(ns shim.cljs.core
  (:require
   [shim.cljs.facts :refer (load-facts os)]
   [shim.cljs.shell :refer (sh)]
   [shim.cljs.download :as d]
   [fipp.edn :refer (pprint)]
   [taoensso.timbre :as timbre :refer-macros  [trace debug info error]]
   [cljs.core.async :as async :refer [<! >! chan go-loop go take!]]
   [cljs.nodejs :as nodejs]))

(nodejs/enable-util-print!)

(def channels (atom {:pkg (chan 10)}))

(defn- run-install [res pkg]
  (case (os)
    :Ubuntu  (sh "apt" "install" pkg "-y")
    :FreeBSD (sh "pkg" "install" "-y" pkg)
    :default (throw  (js/Error. "No matching package provider found for "))))

(defn pkg-consumer [c]
  (go-loop []
    (let [[res pkg resp] (<! c)]
      (debug "running pkg install")
      (take! (run-install pkg res) (fn [r] (go (>! resp r)))))
    (recur)))

(defn install
  ([pkg] (install nil pkg))
  ([res pkg]
   (go
     (let [resp (chan)]
       (>! (@channels :pkg) [res pkg resp])
       (<! resp)))))

(defn download
  ([url dest] (download nil url dest))
  ([res url dest]
   (download nil url dest)))

(defn pretty [res s]
  (info res))

(defn setup
  "Setup our environment"
  []
  (load-facts)
  (pkg-consumer (@channels :pkg)))

(comment
  (setup)
  (take! (install "gt5") (fn [v] (println v)))
  (os))
