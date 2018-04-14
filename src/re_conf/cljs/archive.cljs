(ns re-conf.cljs.archive
  "Archive extraction"
  (:require 
    [re-conf.cljs.common :refer (run)]
    [re-conf.cljs.pkg :refer (install)]
    [re-conf.cljs.shell :refer (exec)]))

(def fs (js/require "fs"))

(defn unzip 
   "Unzip a zip file" 
   [c src dest]
   (if-not (.existsSync fs "/usr/bin/unzip")
     (-> c
       (install "unzip")  
       (exec "/usr/bin/unzip" "-o" src "-d" dest))
     (exec c "/usr/bin/unzip" "-o" src "-d" dest)))
