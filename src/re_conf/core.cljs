(ns re-conf.core
  (:require-macros
   [clojure.core.strint :refer (<<)])
  (:require
   [cuerdas.core :as str]
   [re-conf.resources.facts :refer (os)]
   [re-conf.resources.pkg :as p :refer (initialize)]
   [re-conf.resources.log :refer (info debug error)]
   [cljs.core.async :as async :refer [take! go merge into]]
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

(defn- home
  "Add home to the env"
  [{:keys [user] :as m}]
  (assoc m :home (<< "/home/~{user}")))

(defn invoke
  "invoke function and return as a single channel"
  [n env]
  (into []
        (merge
         (mapv
          (fn [[k f]]
            (debug (<< "invoking ~{k}") ::invoke)
            (go
              (case (arg-count f)
                0 (<! (f))
                1 (<! (f (home env))))))
          (fns n)))))

(comment
  (require 're-base.rcp.docker)
  (initialize)
  (info (invoke re-base.rcp.docker {:user "re-ops" :uid 1000  :gid 1000}) ::done)
  (info (os :platform) ::os))
