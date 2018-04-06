(ns re-conf.cljs.core
  (:require
   [cuerdas.core :as str]
   [re-conf.cljs.facts :refer (os)]
   [re-conf.cljs.shell :refer (sh)]
   [re-conf.cljs.template :as t]
   [re-conf.cljs.log :refer (info debug error)]
   [re-conf.cljs.download :as d]
   [re-conf.cljs.common :refer (channel?)]
   [cljs.core.match :refer-macros  [match]]
   [cljs.core.async :as async :refer [<! >! chan go-loop go take! put!]]
   [cljs.nodejs :as nodejs]))

(nodejs/enable-util-print!)

(def process (js/require "process"))

(defn profile [f]
  (go
    (let [start (.hrtime process)
          r (<! (f))
          end (.hrtime process start)]
      (debug (assoc r :profile end) ::profile)
       r
      )))

(defn run [c next]
  (go
    (let [r (<! c)]
      (if (:ok r)
        (<! (profile next))
        r))))

(def channels (atom {:pkg (chan 10)}))

(defn- run-install [pkg]
  (case (os)
    "linux" (sh "apt" "install" pkg "-y")
    "freebsd" (sh "pkg" "install" "-y" pkg)
    :default (throw  (js/Error. "No matching package provider found for "))))

(defn pkg-consumer [c]
  (go-loop []
    (let [[pkg resp] (<! c)]
      (debug "running pkg install" ::log)
      (take! (run-install pkg) (fn [r] (put! resp r))))
    (recur)))

; resources
(defn checksum
  "Checksum a file and check expected value"
  ([file e k]
   (d/checkum file e k))
  ([c file e k]
   (run c (fn [] (d/checkum file e k)))))

(defn download
  "Download file resource"
  [url dest]
  (d/download url dest))

(defn exec
  "Shell execution resource"
  [a & args]
  (if (channel? a)
    (run a (fn [] (apply sh args)))
    (apply sh (conj args a))))

(defn install
  [pkg]
  (go
    (let [resp (chan)]
      (>! (@channels :pkg) [pkg resp])
      (<! resp))))

(defn template
  "Create a file from a template with args"
  ([args tmpl dest]
   (t/template args tmpl dest))
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
  []
  (go
    (pkg-consumer (@channels :pkg))))

(defn assert-node-major-version
  "Node Major version check"
  []
  (let [version (.-version process)
        major (second (re-find #"v(\d+)\.(\d+)\.(\d+)" version))
        minimum 8]
    (when (> minimum (str/parse-int major))
      (error {:message "Node major version is too old" :version version :required minimum} ::assertion)
      (.exit process 1))))

(defn -main [& args]
  (assert-node-major-version)
  (take! (setup) (fn [r] (info "Started re-conf" ::main))))

(set! *main-cli-fn* -main)

(comment
  (->
   (checksum "/home/ronen/.ackrc" "910d37b2542915dec2f2cb7a0da34f9b" :md5)
   (exec "touch" "/tmp/bla")
   (template {:keys {:key "abcd" :user "foo@bla"}} "resources/authorized_keys.mustache" "/tmp/keys")
   (summary))
  (setup)
  (os))
