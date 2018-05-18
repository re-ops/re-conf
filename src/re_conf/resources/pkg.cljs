(ns re-conf.resources.pkg
  (:refer-clojure :exclude [update key remove])
  (:require-macros
   [clojure.core.strint :refer (<<)])
  (:require
   [re-conf.resources.common :refer (run)]
   [re-conf.resources.log :refer (info debug error channel?)]
   [cljs.core.async :refer [put! take! <! >! go go-loop chan]]
   [re-conf.resources.facts :refer (os)]
   [re-conf.resources.shell :refer (sh)]))

(defprotocol Package
  (install [this pkg])
  (uninstall [this pkg])
  (update- [this])
  (upgrade- [this]))

(defprotocol Repo
  (add-ppa [this repo])
  (rm-ppa [this repo])
  (key- [this k id]))

(defrecord Apt [pipe]
  Package
  (install [this pkg]
    (info "running install" ::apt)
    (go
      (<! (sh "/usr/bin/apt-get" "install" pkg "-y" :sudo true))))

  (uninstall [this pkg]
    (info "running uninstall" ::apt)
    (go
      (<! (sh "/usr/bin/apt-get" "remove" pkg "-y" :sudo true))))

  (update- [this]
    (go
      (<! (sh "/usr/bin/apt-get" "update" :sudo true))))

  (upgrade- [this]
    (go
      (<! (sh "/usr/bin/apt-get" "upgrade" "-y" :sudo true))))

  Repo
  (add-ppa [this repo]
    (go
      (<! (sh "/usr/bin/add-apt-repository" (<< "ppa:~{repo}") "-y" :sudo true))))

  (rm-ppa [this repo]
    (go
      (<! (sh "/usr/bin/add-apt-repository" "--remove" (<< "ppa:~{repo}") "-y" :sudo true))))

  (key- [this server id]
    (go
      (let [{:keys [distro platform]} (<! (os))]
        (if (and (= platform "linux") (= distro "Ubuntu"))
          (<! (sh "/usr/bin/apt-key" "adv" "--keyserver" server "--recv" id :sudo true))
          {:error (<< "cant import apt key under ~{platform} ~{distro}")})))))

(deftype Pkg [pipe]
  Package
  (install [this pkg]
    (go
      (<! (sh "/usr/sbin/pkg" "install" "-y" pkg :sudo true))))

  (uninstall [this pkg]
    (go
      (<! (sh "/usr/sbin/pkg" "remove" "-y" pkg :sudo true))))

  (update- [this]
    (go
      (<! (sh "/usr/sbin/pkg" "update" :sudo true))))

  (upgrade- [this]
    (go
      (<! (sh "/usr/sbin/pkg" "-y" "upgrade" :sudo true)))))

(defn installed? [pkg]
  (go
    (let [{:keys [platform]} (<! (os))]
      (case platform
        "linux" (<! (sh "/usr/bin/dpkg" "-s" pkg))
        :default  {:error (<< "No matching package provider found for ~{platform}")}))))
; pipes

(def pipes (atom {:os (chan 10) :gem (chan 10)}))

(defn os-pipe
  "OS packages pipe"
  []
  (:os @pipes))

(defn gem-pipe
  "gem pipe"
  []
  (:gem @pipes))

; providers
(defn apt []
  (Apt. (os-pipe)))

(defn pkg []
  (Pkg. (os-pipe)))

; consumers
(defn- gem-consumer [c]
  (go-loop []
    (let [[f provider args resp] (<! c)]
      (debug (<< "running gem ~{f} ~{args}") ::gem-consumer)
      (>! resp (<! (apply f provider args))))
    (recur)))

(defn- pkg-consumer [c]
  (go-loop []
    (let [[f provider args resp] (<! c)
          result (<! (apply f provider args))]
      (>! resp result))
    (recur)))

(defn- call [f provider & args]
  (go
    (let [resp (chan)]
      (>! (:pipe provider) [f provider args resp])
      (<! resp))))

(defn into-spec [m args]
  (if (empty? args)
    m
    (let [a (first args)]
      (cond
        (string? a) (into-spec (assoc m :pkg a) (rest args))
        (channel? a) (into-spec (assoc m :ch a) (rest args))
        (keyword? a) (into-spec (assoc m :state a) (rest args))
        (fn? a) (into-spec (assoc m :provider (a)) (rest args))))))

(defn package
  "Package resource with optional provider and state parameters"
  ([& args]
   (let [{:keys [ch pkg state provider] :or {provider (apt) state :present}} (into-spec {} args)
         fns {:present install :absent uninstall}]
     (if ch
       (run ch #(call (fns state) provider pkg))
       (call (fns state) provider pkg)))))

(defn update
  "Update packages"
  ([]
   (update (apt)))
  ([provider]
   (call update- provider))
  ([c provider]
   (run c #(update provider))))

(defn upgrade
  "Upgrade packages"
  ([]
   (upgrade (apt)))
  ([provider]
   (call upgrade- provider))
  ([c provider]
   (run c #(upgrade provider))))

(defn ppa
  "Add an Ubuntu PPA repository"
  ([repo]
   (ppa repo :present))
  ([repo state]
   (let [fns {:present add-ppa :absent rm-ppa}]
     (call (fns state) (apt) repo)))
  ([c repo state]
   (run c #(ppa repo state))))

(defn key
  "Import a gpg apt key"
  ([server id]
   (call key- (apt) [server id]))
  ([c server id]
   (run c #(key server id))))

(defn initialize
  "Setup the serializing go loop for package management access"
  []
  (go
    (pkg-consumer (os-pipe))
    (gem-consumer (gem-pipe))))

(comment
  (initialize)
  (info (package "git" :absent) ::remove)
  (info (package "git") ::add)
  (info (update) ::update))
