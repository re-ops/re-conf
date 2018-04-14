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

(def serialize (atom (chan 10)))

(defn- run-install [pkg]
  (go
    (let [{:keys [platform]} (<! (os))]
      (case platform
        "linux" (<! (sh "/usr/bin/apt-get" "install" pkg "-y" :sudo true))
        "freebsd" (<! (sh "pkg" "install" "-y" pkg :sudo true))
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
  [repo]
  (go
    (let [{:keys [distro platform]} (<! (os))]
      (if (and (= platform "linux") (= distro "Ubuntu"))
         (<! (sh "/usr/bin/add-apt-repository" (<< "ppa:~{repo}") "-y" :sudo true))
         {:error (<< "ppa isn't supported under ~{platform} ~{distro}")}))))

(defn- run-key
  "Add an apt key"
  [[server id]]
  (go
    (let [{:keys [distro platform]} (<! (os))]
      (if (and (= platform "linux") (= distro "Ubuntu"))
         (<! (sh "/usr/bin/apt-key" "adv" "--keyserver" server "--recv" id :sudo true))
         {:error (<< "cant import apt key under ~{platform} ~{distro}")}))))


(defn pkg-consumer [c]
  (go-loop []
    (let [[action args resp] (<! c)]
      (debug (<< "running ~{action} ~{args}") ::pkg-consumer)
      (case action
        :install (>! resp (<! (run-install args)))
        :update  (>! resp (<! (run-update)))
        :upgrade  (>! resp (<! (run-upgrade)))
        :ppa  (>! resp (<! (run-ppa args)))
        :key  (>! resp (<! (run-key args)))
        ))
    (recur)))

(defn- call [action args]
  (go
    (let [resp (chan)]
      (>! @serialize [action args resp])
      (<! resp))))

(defn install
  "Install a package"
  ([pkg]
    (call :install pkg))
  ([c pkg]
   (run c #(call :install pkg))))

(defn update
  "Update package manager metadata"
  ([]
    (call :update nil))
  ([c]
   (run c #(call :update nil))))

(defn upgrade
  "Upgrade all installed packages"
  ([]
    (call :upgrade nil))
  ([c]
   (run c #(call :upgrade nil))))

(defn ppa
  "Add an Ubuntu PPA repository"
  ([repo]
    (call :ppa repo))
  ([c repo]
   (run c #(call :ppa repo))))

(defn key
  "Import a gpg apt key"
  ([server id]
    (call :key [server id]))
  ([c server id]
   (run c #(call :key [server id]))))

(defn initialize
  "Setup the serializing go loop for package management access"
  []
  (go
    (pkg-consumer @serialize)))

(comment
  (info (key "keyserver.ubuntu.com" "42ED3C30B8C9F76BC85AC1EC8B095396E29035F0") ::key)
  (initialize)
  (info (install "zsh") ::install)
  (info (update) ::update)
  (info (upgrade) ::update)
  )
