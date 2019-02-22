(ns re-conf.core
  (:require-macros
   [clojure.core.strint :refer (<<)])
  (:require
   [cuerdas.core :as str]
   [re-conf.resources.common :refer (ok?)]
   [re-conf.resources.facts :refer (os)]
   [re-conf.resources.pkg :as p :refer (initialize)]
   [re-conf.resources.log :refer (info debug error channel?)]
   [cljs.core.async :as async :refer [take! go chan]]
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

(defn check-result [r]
  (if (channel? r)
    r
    (go {:error "recipe function didn't return a channel!"})))

(defn call-fn [env [k f]]
  (debug (<< "invoking ~{k}") ::invoke)
  (go
    (case (arg-count f)
      0 (<! (check-result (f)))
      1 (<! (check-result (f env))))))

(defn- invoke
  "Invoke public functions in a namespace and return results
    (invoke re-base.rcp.backup env)
  "
  [env n]
  (async/into [] (async/merge (mapv (partial call-fn env) (fns n)))))

(defn invoke-all
  "Invoke multiple namespace functions and return errors"
  [env nmsps]
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

(defn apply*
  "Take a resource and apply it on a sequence:
     * In case of an error returning all errors under error key
     * In case of ok returning all results under ok key

     (apply* c directory [\"tmp/1\" \"/tmp/2\"])
     (apply* c directory (fn [f] [f :absent]) [\"tmp/1\" \"/tmp/2\"]) ; transforming args function
   "
  ([c r as]
   (go
     (let [pre (<! c)]
       (if-not (ok? pre)
         pre
         (let [results (<! (async/into [] (async/merge (map (fn [args] (apply r args)) as))))]
           (if-let [error (first (filter :error results))]
             {:error (mapv :error (filter :error results))}
             {:ok (mapv :ok results)}))))))
  ([c r f as]
   (apply* c r (map f as))))

(comment
  (initialize)
  (require 're-base.rcp.docker))
