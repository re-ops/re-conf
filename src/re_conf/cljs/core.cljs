(ns re-conf.cljs.core
  (:require-macros
   [clojure.core.strint :refer (<<)])
  (:require
   [cuerdas.core :as str]
   [re-conf.cljs.facts :refer (os)]
   [re-conf.cljs.pkg :as p :refer (initialize)]
   [re-conf.cljs.log :refer (info debug error)]
   [re-conf.cljs.common :refer (run)]
   [cljs.core.match :refer-macros  [match]]
   [cljs.core.async :as async :refer [<! >! chan go go-loop take! put!]]
   [cljs.nodejs :as nodejs]))

(nodejs/enable-util-print!)

(def process (js/require "process"))

(defn summary
  "Print result"
  ([c]
   (summary c "Pipeline ok"))
  ([c m]
   (take! c
          (fn [r]
            (match r
              {:error e} (error e ::summary-fail)
              {:ok o} (info m ::summary-ok)
              :else (error r ::summary-error))))))

(defn assert-node-major-version
  "Node Major version check"
  []
  (let [version (.-version process)
        major (second (re-find #"v(\d+)\.(\d+)\.(\d+)" version))
        minimum 8]
    (when (> minimum (str/parse-int major))
      (error {:message "Node major version is too old" :version version :required minimum} ::assertion)
      (.exit process 1))))

(defn invoke
  "Invoking all public fn in ns concurrently"
  [n]
  (doseq [[k v] (js->clj (js/Object n))]
    (debug (<< "invoking ~{k}") ::invoke)
    (go (.call v))))

(defn -main [& args]
  (assert-node-major-version)
  (take! (initialize)
         (fn [r]
           (info "Started re-conf" ::main)
           #_(invoke re-conf.rcp.basic))))

(set! *main-cli-fn* -main)

(comment
  (invoke re-conf.rcp.basic)
  (initialize)
  (info (os :platform) ::os))
