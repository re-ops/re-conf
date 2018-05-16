(ns re-conf.cljs.git
  "Git resources"
  (:refer-clojure :exclude [clone])
  (:require-macros
   [clojure.core.strint :refer (<<)])
  (:require
   [clojure.string :refer (includes?)]
   [cljs-node-io.core :as io]
   [re-conf.cljs.shell :refer (sh)]
   [re-conf.cljs.facts :refer (os)]
   [re-conf.cljs.log :refer (info debug error)]
   [re-conf.cljs.common :refer (run)]
   [cljs.core.async :refer [<! go]]))

(def fs (js/require "fs"))

(defn- binary
  "Grab git binary path"
  []
  (go
    (let [{:keys [platform]} (<! (os))]
      (case platform
        "linux" "/usr/bin/git"
        "freebsd" "/usr/local/bin/git"
        :default  {:error (<< "No matching git binary path found for ~{platform}")}))))

(defn- repo-exists?
   [repo path]
   (when (.existsSync fs path)
     (includes? (io/slurp (<< "~{path}/.git/config")) repo)))

(defn run-pull
  "Pull implementation"
  [repo dest]
  (go
    (if (repo-exists? repo dest)
      (let [git (<! (binary))]
        (<! (sh git (<< "--git-dir=~{dest}.git") "pull")))
      (debug (<< "Skipping pull ~{repo} is missing under ~{dest}") ::git-clone)
      )))

(defn run-clone
  "Clone implementation"
  [repo dest]
  (go
    (if-not (repo-exists? repo dest)
      (let [git (<! (binary))]
        (<! (sh git "clone" repo dest)))
      (debug (<< "Skipping clone ~{repo} exists under ~{dest}") ::git-clone)
      )))

(defn clone
  "Clone a git repo resource"
  ([repo dest]
   (run-clone repo dest))
  ([c repo dest]
   (run c #(clone repo dest))))

(defn pull
  "Pull latest changes from a git repo resource"
  ([repo dest]
   (run-pull repo dest))
  ([c repo dest]
   (run c #(pull repo dest))))
