(ns re-conf.cljs.pkg
  (:refer-clojure :exclude [update key])
  (:require-macros
   [clojure.core.strint :refer (<<)])
  (:require
   [re-conf.cljs.common :refer (run)]
   [re-conf.cljs.log :refer (info debug error)]
   [cljs.core.async :refer [put! take! <! >! go go-loop chan]]
   [re-conf.cljs.facts :refer (os)]
   [re-conf.cljs.shell :refer (sh)]))

(def pipes (atom {:apt (chan 10) :gem (chan 10)}))

(defn- run-remove [pkg]
  (go
    (let [{:keys [platform]} (<! (os))]
      (case platform
        "linux" (<! (sh "/usr/bin/apt-get" "remove" pkg "-y" :sudo true))
        "freebsd" (<! (sh "pkg" "remove" "-y" pkg :sudo true))
        :default  {:error (<< "No matching package provider found for ~{platform}")}))))

(defn- run-pkg [[pkg state]]
  (go
    (let [{:keys [platform]} (<! (os))]
      (case platform
        "linux" (<! (sh "/usr/bin/apt-get" (name state) pkg "-y" :sudo true))
        "freebsd" (<! (sh "pkg" (name state) "-y" pkg :sudo true))
        :default  {:error (<< "No matching package provider found for ~{platform}")}))))

(defn- run-update []
  (go
    (let [{:keys [platform]} (<! (os))]
      (case platform
        "linux" (<! (sh "/usr/bin/apt-get" "update" :sudo true))
        "freebsd" (<! (sh "pkg" "update" :sudo true))
        :default  {:error (<< "No matching package provider found for ~{platform}")}))))

(defn- run-upgrade []
  (go
    (let [{:keys [platform]} (<! (os))]
      (case platform
        "linux" (<! (sh "/usr/bin/apt-get" "upgrade" "-y" :sudo true))
        "freebsd" (<! (sh "pkg" "upgrade" "-y" :sudo true))
        :default  {:error (<< "No matching package provider found for ~{platform}")}))))

(defn- run-ppa
  "Add a ppa repository"
  [[repo state]]
  (go
    (let [{:keys [distro platform]} (<! (os)) flags (if (= state :remove) "--remove" "")]
      (if (and (= platform "linux") (= distro "Ubuntu"))
        (<! (sh "/usr/bin/add-apt-repository" flags (<< "ppa:~{repo}") "-y" :sudo true))
        {:error (<< "ppa isn't supported under ~{platform} ~{distro}")}))))

(defn- run-key
  "Add an apt key"
  [[server id]]
  (go
    (let [{:keys [distro platform]} (<! (os))]
      (if (and (= platform "linux") (= distro "Ubuntu"))
        (<! (sh "/usr/bin/apt-key" "adv" "--keyserver" server "--recv" id :sudo true))
        {:error (<< "cant import apt key under ~{platform} ~{distro}")}))))

(defn- gem-consumer [c]
  (go-loop []
    (let [[action args resp] (<! c)]
      (debug (<< "running gem ~{:action} ~{args}") ::gem-consumer)
      (>! resp (<! (run-pkg args))))
    (recur)))

(defn- apt-consumer [c]
  (go-loop []
    (let [[action args resp] (<! c)]
      (debug (<< "running ~{action} ~{args}") ::apt-consumer)
      (case action
        :pkg (>! resp (<! (run-pkg args)))
        :ppa  (>! resp (<! (run-ppa args)))
        :update  (>! resp (<! (run-update)))
        :upgrade  (>! resp (<! (run-upgrade)))
        :key  (>! resp (<! (run-key args)))))
    (recur)))

(defn- call [pipe action args]
  (go
    (let [resp (chan)]
      (>! pipe [action args resp])
      (<! resp))))

(defn- apt-call [action args]
  (call (@pipes :apt) action args))

(defn- gem-call [action args]
  (call (@pipes :gem) action args))

(def states {:present :install
             :absent :remove})

(defn gem
  "Install a Ruby gem"
  ([pkg]
   (gem pkg :present))
  ([pkg state]
   (gem-call :gem [pkg state]))
  ([c pkg state]
   (run c #(gem pkg state))))

(defn package
  "Install a package"
  ([pkg]
   (package pkg :present))
  ([pkg state]
   (apt-call :pkg [pkg state]))
  ([c pkg state]
   (run c #(package pkg state))))

(defn update
  "Update package manager metadata"
  ([]
   (apt-call :update nil))
  ([c]
   (run c #(update))))

(defn upgrade
  "Upgrade all installed packages"
  ([]
   (apt-call :upgrade nil))
  ([c]
   (run c #(upgrade))))

(defn ppa
  "Add an Ubuntu PPA repository"
  ([repo]
   (ppa repo :present))
  ([repo state]
   (apt-call :ppa [repo state]))
  ([c repo state]
   (run c #(ppa repo state))))

(defn key
  "Import a gpg apt key"
  ([server id]
   (apt-call :key [server id]))
  ([c server id]
   (run c #(key server id))))

(defn initialize
  "Setup the serializing go loop for package management access"
  []
  (go
    (apt-consumer (@pipes :apt))
    (gem-consumer (@pipes :gem))))

(comment
  (info (key "keyserver.ubuntu.com" "42ED3C30B8C9F76BC85AC1EC8B095396E29035F0") ::key)
  (initialize)
  (info (package "zsh" :present) ::install)
  (info (update) ::update)
  (info (upgrade) ::update))
