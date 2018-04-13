(ns re-conf.cljs.pkg
  (:require-macros
   [clojure.core.strint :refer (<<)])
  (:require
   [re-conf.cljs.log :refer (info debug error)]
   [cljs.core.async :refer [put! take! <! >! go go-loop chan]]
   [re-conf.cljs.facts :refer (os)]
   [re-conf.cljs.shell :refer (sh)]))

(def serialize (chan 10))

(defn- run-install [pkg]
  (go
    (let [{:keys [platform]} (<! (os))]
      (case platform
        "linux" (<! (sh "/usr/bin/apt" "install" pkg "-y" :sudo true))
        "freebsd" (<! (sh "pkg" "install" "-y" pkg :sudo true))
        :default  {:error (<< "No matching package provider found for ~{platform}")}))))

(defn- run-update []
  (go
    (let [{:keys [platform]} (<! (os))]
      (case platform
        "linux" (<! (sh "/usr/bin/apt" "update" :sudo true))
        "freebsd" (<! (sh "pkg" "update" :sudo true))
        :default  {:error (<< "No matching package provider found for ~{platform}")}))))

(defn pkg-consumer [c]
  (go-loop []
    (let [[action args resp] (<! c)]
      (debug (<< "running ~{action} ~{args}") ::pkg-consumer)
      (case action
        :install (>! resp (<! (run-install args)))
        :update  (>! resp (<! (run-update)))))
    (recur)))

(defn- call [action args]
  (go
    (let [resp (chan)]
      (>! serialize [action args resp])
      (<! resp))))

(defn install
  "install a package"
  [pkg]
  (call :install pkg))

(defn update-
  "update package manager"
  []
  (call :update nil))

(defn initialize
  "Setup the serializing go loop for package management access"
  []
  (go
    (pkg-consumer serialize)))

(comment
  (initialize)
  (info (install "tmux") ::install)
  (info (update-) ::update)
  (info (update-) ::update))
