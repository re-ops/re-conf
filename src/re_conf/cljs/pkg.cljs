(ns re-conf.cljs.pkg
  (:require
   [re-conf.cljs.log :refer (info debug error)]
   [cljs.core.async :refer [put! take! <! >! go go-loop chan]]
   [re-conf.cljs.facts :refer (os)]
   [re-conf.cljs.shell :refer (sh)]))

(def serialize (chan 10))

(defn- run-install [pkg]
  (case (os)
    "linux" (sh "apt" "install" pkg "-y")
    "freebsd" (sh "pkg" "install" "-y" pkg)
    :default (throw  (js/Error. "No matching package provider found for "))))

(defn pkg-consumer [c]
  (go-loop []
    (let [[pkg resp] (<! c)]
      (debug "running pkg install" ::log)
      (take! (run-install pkg) (fn [r] (put! resp r))))
    (recur)))

(defn install
  "install "
  [pkg]
  (go
    (let [resp (chan)]
      (>! serialize [pkg resp])
      (<! resp))))

(defn initialize
  "Setup the serializing go loop for package management access"
  []
  (go
    (pkg-consumer serialize)))
