(ns shim.cljs.core
  (:require
     [shim.cljs.shell :refer (sh)]
     [fipp.edn :refer (pprint)]
     [taoensso.timbre :as timbre :refer-macros  [trace debug info error]]
     [cljs.core.async :as async :refer [<! go]]
     [cljs.nodejs :as nodejs]))

(nodejs/enable-util-print!)

(def results (atom {}))

(defn install
  ([p] (install nil p))
  ([res p]
     {:result :ok}))

(defn pretty [res s]
   (info res))

(defn into-json [s]
  (.parse js/JSON s))

(defn load-facts []
  (go
    (let [{:keys [out]} (<! (sh "facter" "--json"))
          facts (js->clj (into-json out) :keywordize-keys true)]
      (swap! results assoc :facter facts))))

(comment
 (debug "bla")
 (pprint (get-in @results [:facter :os :name]) )
 (load-facts)
 )
