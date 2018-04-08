(ns re-conf.cljs.core
  (:require-macros
   [clojure.core.strint :refer (<<)])
  (:require
   [clojure.repl :as rpl]
   [cuerdas.core :as str]
   [re-conf.cljs.facts :refer (os)]
   [re-conf.cljs.pkg :refer (initialize)]
   [re-conf.cljs.shell :refer (sh)]
   [re-conf.cljs.template :as t]
   [re-conf.cljs.log :refer (info debug error)]
   [re-conf.cljs.download :as d]
   [re-conf.cljs.common :refer (channel?)]
   [cljs.core.match :refer-macros  [match]]
   [cljs.core.async :as async :refer [<! >! chan go go-loop take! put!]]
   [cljs.nodejs :as nodejs]))

(nodejs/enable-util-print!)

(def process (js/require "process"))

(defn profile [f]
  (go
    (let [start (.hrtime process)
          r (<! (f))
          end (.hrtime process start)]
      (debug (assoc r :profile end) ::profile)
      r)))

(defn run [c f]
  (go
    (let [r (if c (<! c) {:ok true})]
      (if (:ok r)
        (<! (profile f))
        r))))

; resources
(defn checksum
  "Checksum a file and check expected value"
  ([file e k]
   (checksum nil file e k))
  ([c file e k]
   (run c #(d/checkum file e k))))

(defn download
  "Download file resource"
  ([url dest]
   (download nil url dest))
  ([c url dest]
   (run c #(d/download url dest))))

(defn exec
  "Shell execution resource"
  [a & args]
  (if (channel? a)
    (run a  #(apply sh args))
    (run nil #(apply sh (conj args a)))))

(defn template
  "Create a file from a template with args"
  ([args tmpl dest]
   (template nil args tmpl dest))
  ([c args tmpl dest]
   (run c (fn [] (t/template args tmpl dest)))))

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

(defn setup
  "Setup our environment"
  [])

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
           (invoke re-conf.cljs.basic))))

(set! *main-cli-fn* -main)

(comment
  (name re-conf.cljs.basic)
  (invoke re-conf.cljs.basic)
  (setup)
  (os))
