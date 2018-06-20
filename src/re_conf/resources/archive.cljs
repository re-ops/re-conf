(ns re-conf.resources.archive
  "Archive extraction"
  (:require
   [re-conf.resources.common :refer (run)]
   [re-conf.resources.pkg :refer (package)]
   [re-conf.resources.shell :refer (exec)]))

(def fs (js/require "fs"))

(defn unzip
  "Unzip resource"
  [c src dest]
  (if-not (.existsSync fs "/usr/bin/unzip")
    (-> c
        (package "unzip")
        (exec "/usr/bin/unzip" "-o" src "-d" dest))
    (exec c "/usr/bin/unzip" "-o" src "-d" dest)))

(defn bzip2
  "bzip resource"
  [c target]
  (if-not (.existsSync fs "/usr/bin/bzip2")
    (-> c
        (package "bzip2")
        (exec "/bin/bzip2" "-kf" "-d" target))
    (exec c "/bin/bzip2" "-kf" "-d" target)))

(defn untar
  "untar resource"
  [c src dest]
  (exec c "/bin/tar" "-xzf" src "-C" dest))
