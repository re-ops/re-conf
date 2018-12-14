(ns re-conf.resources.archive
  "Archive extraction and manipulation resources"
  (:require
   [re-conf.resources.pkg :refer (package)]
   [re-conf.resources.shell :refer (exec)]))

(def fs (js/require "fs"))

(defn unzip
  "Unzip resource:

    (unzip \"foo.zip\" \"/tmp/foo\")
  "
  [c src dest]
  (if-not (.existsSync fs "/usr/bin/unzip")
    (-> c
        (package "unzip")
        (exec "/usr/bin/unzip" "-o" src "-d" dest))
    (exec c "/usr/bin/unzip" "-o" src "-d" dest)))

(defn bzip2
  "bzip2 extraction resource:

    (bzip2 \"foo.bz2\")
  "
  [c target]
  (if-not (.existsSync fs "/usr/bin/bzip2")
    (-> c
        (package "bzip2")
        (exec "/bin/bzip2" "-kf" "-d" target))
    (exec c "/bin/bzip2" "-kf" "-d" target)))

(defn untar
  "Untar resource:

    (untar \"foo.tar\" \"/tmp/foo\")
  "
  [c src dest]
  (exec c "/bin/tar" "-xzf" src "-C" dest))
