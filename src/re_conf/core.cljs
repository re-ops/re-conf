(ns re-conf.core
  (:require-macros
   [clojure.core.strint :refer (<<)])
  (:require
   [cuerdas.core :as str]
   [re-conf.resources.facts :refer (os)]
   [re-conf.resources.pkg :as p :refer (initialize)]
   [re-conf.resources.log :refer (info debug error)]
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

(defn- home
  "Add home to the env"
  [{:keys [users] :as m}]
   (let [{:keys [name]} (users :main)]
     (assoc m :home (<< "/home/~{name}"))))

(defn- main-user
  "Add main user to env root"
  [{:keys [users] :as m}]
  (merge m (users :main)))

(defn call-fn [env [k f]]
  (debug (<< "invoking ~{k}") ::invoke)
  (go
    (case (arg-count f)
      0 (<! (f))
      1 (<! (f (-> env home main-user))))))

(defn- invoke
  "Invoke public functions in a namespace and return results
    (invoke re-base.rcp.backup env)
  "
  [env n]
  (async/into [] (async/merge (mapv (partial call-fn env) (fns n)))))

(defn invoke-all
  "Invoke multiple namespace functions and return errors"
  [env & nmsps]
  (go
    (let [results (<! (async/into [] (async/merge (map (partial invoke env) nmsps))))]
      (mapcat (fn [rs] (filter :error rs)) results))))

(defn report-n-exit [c]
  (go
    (let [errors (<! c)]
      (doseq [e errors]
        (error (<< "errors found in ~{(:context e)}") ::errors-report))
      (when-not (empty? errors)
        (error (<< "provision script failed due to ~(count errors) errors, check error logs exit 1.") ::exit)
        (.exit process 1)))))


(comment
  (initialize)
  (require 're-base.rcp.docker))
