(ns shim.cljs.core
  (:require
     [shim.cljs.facts :refer (os)]
     [shim.cljs.shell :refer (sh)]
     [fipp.edn :refer (pprint)]
     [taoensso.timbre :as timbre :refer-macros  [trace debug info error]]
     [cljs.core.async :as async :refer [<!]]
     [cljs.nodejs :as nodejs]))

(nodejs/enable-util-print!)

(def results (atom {}))

(defn install
  ([pkg] (install nil pkg))
  ([res pkg]
   (case (os)
     :Ubuntu  (sh "apt" "install" pkg "-y")
     )
   ))

(defn pretty [res s]
   (info res))

