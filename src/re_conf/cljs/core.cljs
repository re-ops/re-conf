(ns re-conf.cljs.core
  (:require-macros
   [clojure.core.strint :refer (<<)])
  (:require
   [cuerdas.core :as str]
   [re-conf.cljs.facts :refer (os)]
   [re-conf.cljs.pkg :as p :refer (initialize)]
   [re-conf.cljs.log :refer (info debug error)]
   [cljs.core.async :as async :refer [take! go]]
   [cljs.nodejs :as nodejs]))

(nodejs/enable-util-print!)

(def process (js/require "process"))

(defn assert-node-major-version
  "Node Major version check"
  []
  (let [version (.-version process)
        major (second (re-find #"v(\d+)\.(\d+)\.(\d+)" version))
        minimum 8]
    (when (> minimum (str/parse-int major))
      (error {:message "Node major version is too old" :version version :required minimum} ::assertion)
      (.exit process 1))))

(defn- arg-count
  "How many arguments the function expects to get"
  [f]
  (.-length f))

(defn- fns
  "Get public functions from a ns"
  [n]
  (js->clj (js/Object n)))

(defn invoke
  "Invoking all public fn in ns concurrently"
  [n env]
  (doseq [[k f] (fns n)]
    (debug (<< "invoking! ~{k} ~{env} ~(arg-count f)") ::invoke)
    (go
      (case (arg-count f)
         0 (f)
         1 (f env)))))

(comment
  (initialize)
  (info (os :platform) ::os))
