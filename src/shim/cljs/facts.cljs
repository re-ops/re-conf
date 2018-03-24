(ns shim.cljs.facts
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
     [shim.cljs.shell :refer (sh)]
     [fipp.edn :refer (pprint)]
     [taoensso.timbre :as timbre :refer-macros  [trace debug info error]]
     [cljs.core.async :as async :refer [<!]]
     ))

(def facts (atom nil))

(defn into-json [s]
  (.parse js/JSON s))

(defn load-facts []
  (go
    (let [{:keys [out]} (<! (sh "facter" "--json"))
          fs (js->clj (into-json out) :keywordize-keys true)]
      (reset! facts fs))))

(defn os []
  (keyword (get-in @facts [:os :name])))

(comment
 (load-facts)
 (os)
 (keyword (get-in @facts [:os :name]))
 )
