(ns re-conf.cljs.facts
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [re-conf.cljs.shell :refer (sh)]
   [re-conf.cljs.log :refer (error)]
   [fipp.edn :refer (pprint)]
   [cljs.core.async :as async :refer [<!]]))

(def facts (atom nil))

(defn into-json [s]
  (.parse js/JSON s))

(defn load-facts []
  (go
    (let [{:keys [out err exit]} (<! (sh "facter" "--json"))
          fs (js->clj (into-json out) :keywordize-keys true)]
      (if-not (= exit 0)
        (error err ::log)
        (reset! facts fs)))))

(defn os []
  (when-not @facts
    (throw (js/Error. "facts not initialized")))
  (keyword (get-in @facts [:os :name])))

(comment
  (load-facts)
  (os))
