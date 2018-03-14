(ns shim.cljs.core
  (:require
     [shim.cljs.shell :refer (sh)]
     [fipp.edn :refer (pprint)]
     [taoensso.timbre :as timbre :refer-macros  [trace debug info error]]
     [cljs.nodejs :as nodejs]))

(nodejs/enable-util-print!)

(defn install
  ([p] (install nil p))
  ([res p]
     {:result :ok}))

(defn pretty [res s]
   (info res))

(defn load-facts []
   (let [{:keys [out]} (sh "facter" "--json")]
     (js->clj out)))

(defn facts
  "facter facts"
   []
   )

(comment
 (debug "bla")
 (pprint {:one 1})
 (load-facts)
 )
