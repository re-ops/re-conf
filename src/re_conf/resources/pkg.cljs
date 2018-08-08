(ns re-conf.resources.pkg
  "Package resources"
  (:refer-clojure :exclude [update key remove])
  (:require-macros
   [clojure.core.strint :refer (<<)])
  (:require
   [clojure.string :refer (join)]
   [re-conf.spec.pkg :refer (fingerprint)]
   [re-conf.resources.download :refer (download)]
   [re-conf.resources.common :refer (run)]
   [re-conf.spec.file :refer (contains)]
   [re-conf.resources.log :refer (info debug error channel?)]
   [cljs.core.async :refer [put! take! <! >! go go-loop chan]]
   [re-conf.resources.facts :refer (os)]
   [re-conf.resources.shell :refer (sh)]))

(defprotocol Package
  (install- [this pkg])
  (uninstall [this pkg])
  (update- [this])
  (upgrade- [this]))

(defprotocol Repo
  (add-repo- [this repo])
  (rm-repo [this repo])
  (key-file- [this file])
  (key-server- [this server id]))

(def apt-bin "/usr/bin/apt-get")

(defrecord Apt [pipe]
  Package
  (install- [this pkg]
    (debug "running install" ::apt)
    (go
      (<! (apply sh (flatten [apt-bin "install" pkg "-y"])))))

  (uninstall [this pkg]
    (debug "running uninstall" ::apt)
    (go
      (<! (apply sh (flatten ["/usr/bin/apt-get" "remove" pkg "-y"])))))

  (update- [this]
    (go
      (<! (sh "/usr/bin/apt-get" "update"))))

  (upgrade- [this]
    (go
      (<! (sh "/usr/bin/apt-get" "upgrade" "-y"))))

  Repo
  (add-repo- [this repo]
    (go
      (let [{:keys [present error]} (<! (contains "/etc/apt/sources.list" line))]
        (if (and error (nil? present))
          error
          (if present
            {:ok (<< "repo ~{repo} is present, skipping") :skip true}
            (<! (sh "/usr/bin/add-apt-repository" repo "-y")))))))

  (rm-repo [this repo]
    (go
      (<! (sh "/usr/bin/add-apt-repository" "--remove" repo "-y"))))

  (key-server-
    [this server id]
    (go
      (let [{:keys [distro platform]} (<! (os))]
        (if (and (= platform "linux") (= distro "Ubuntu"))
          (<! (sh "/usr/bin/apt-key" "adv" "--keyserver" server "--recv" id))
          {:error (<< "cant import apt key under ~{platform} ~{distro}")}))))

  (key-file-
    [this file]
    (go
      (let [{:keys [distro platform]} (<! (os))]
        (if (and (= platform "linux") (= distro "Ubuntu"))
          (<! (sh "/usr/bin/apt-key" "add" file))
          {:error (<< "cant import apt key under ~{platform} ~{distro}")})))))

(deftype Pkg [pipe]
  Package
  (install- [this pkg]
    (go
      (<! (sh "/usr/sbin/pkg" "install" (join " " pkg) "-y" pkg))))

  (uninstall [this pkg]
    (go
      (<! (sh "/usr/sbin/pkg" "remove" "-y" (join " " pkg)))))

  (update- [this]
    (go
      (<! (sh "/usr/sbin/pkg" "update"))))

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
        (string? a) (into-spec (clojure.core/update m :pkg (fn [v] (conj v a))) (rest args))
        (channel? a) (into-spec (assoc m :ch a) (rest args))
        (keyword? a) (into-spec (assoc m :state a) (rest args))
        (fn? a) (into-spec (assoc m :provider (a)) (rest args))))))

(defn package
  "Package resource with optional provider and state parameters:

    (package \"ghc\") ; state is present by default
    (package \"ghc\" \"gnome-terminal\") ; multiple packages
    (package \"ghc\" :present) ; explicit state
    (package \"ghc\" :absent) ; remove package
  "
  ([& args]
   (let [{:keys [ch pkg state provider] :or {provider (apt) state :present}} (into-spec {} args)
         fns {:present install- :absent uninstall}]
     (if ch
       (run ch #(call (fns state) provider pkg))
       (call (fns state) provider pkg)))))

(defn update
  "Update package repository index resource:

    (update)
  "
  ([]
   (update nil))
  ([c]
   (update c (apt)))
  ([c provider]
   (run c #(call update- provider))))

(defn upgrade
  "Upgrade installed packages:

    (upgrade)
  "
  ([]
   (upgrade (apt)))
  ([provider]
   (call upgrade- provider))
  ([c provider]
   (run c #(upgrade provider))))

(defn repository
  "Package repository resource:

    (repository \"deb https://raw.githubusercontent.com/narkisr/fpm-barbecue/repo/packages/ubuntu/ xenial main\" :present)
    (repository \"deb https://raw.githubusercontent.com/narkisr/fpm-barbecue/repo/packages/ubuntu/ xenial main\" :absent)
   "
  ([repo]
   (repository repo :present))
  ([repo state]
   (let [fns {:present add-repo- :absent rm-repo}]
     (call (fns state) (apt) repo)))
  ([c repo state]
   (run c #(repository repo state))))

(defn key-file
  "Import a gpg apt key from a file resource:

     (key-file \"key.gpg\")
   "
  ([file]
   (call key-file- (apt) file))
  ([c file]
   (run c #(key-file file))))

(defn key-server
  "Import a gpg apt key from a gpg server resource:

     (key-server \"keyserver.ubuntu.com\" \"42ED3C30B8C9F76BC85AC1EC8B095396E29035F0\")
   "
  ([server id]
   (call key-server- (apt) server id))
  ([c server id]
   (run c #(key-server server id))))

(defn add-repo
  "Add repo, gpg key and fingerprint:

   (let [repo \"deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main\"
         key \"https://dl-ssl.google.com/linux/linux_signing_key.pub\"]
      (add-repo repo key \"7FAC5991\"))
  "
  [repo url id]
  (let [dest (<< "/tmp/~{id}.key")]
    (->
     (download url dest)
     (key-file dest)
     (fingerprint id)
     (repository repo :present)
     (update))))

(defn initialize
  "Setup the serializing go loop for package management access"
  []
  (go
    (pkg-consumer (os-pipe))
    (gem-consumer (gem-pipe))))

