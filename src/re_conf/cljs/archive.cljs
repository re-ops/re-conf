(ns re-conf.cljs.archive
  "Archive extraction"
  (:require
   [re-conf.cljs.common :refer (run)]
   [re-conf.cljs.pkg :refer (package)]
   [re-conf.cljs.shell :refer (exec)]))

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
  [c dest]
  (if-not (.existsSync fs "/usr/bin/bzip2")
    (-> c
        (package "bzip2")
        (exec "/bin/bzip2" "-f" "-d" dest))
    (exec "/bin/bzip2" "-f" "-d" dest)))
