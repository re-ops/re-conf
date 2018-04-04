(ns re-conf.cljs.core
  (:require
   [re-conf.cljs.facts :refer (load-facts os)]
   [re-conf.cljs.shell :refer (sh)]
   [re-conf.cljs.log :refer (info debug error)]
   [re-conf.cljs.download :as d]
   [cljs.core.match :refer-macros  [match]]
   [cljs.core.async :as async :refer [<! >! chan go-loop go take! put!]]
   [cljs.nodejs :as nodejs]))

(nodejs/enable-util-print!)

(def channels (atom {:pkg (chan 10)}))

(defn- run-install [res pkg]
  (case (os)
    :Ubuntu  (sh "apt" "install" pkg "-y")
    :FreeBSD (sh "pkg" "install" "-y" pkg)
    :default (throw  (js/Error. "No matching package provider found for "))))

(defn pkg-consumer [c]
  (go-loop []
    (let [[res pkg resp] (<! c)]
      (debug "running pkg install" ::log)
      (take! (run-install pkg res) (fn [r] (put! resp r))))
    (recur)))

(defn install
  [pkg]
  (go
    (let [resp (chan)]
      (>! (@channels :pkg) [res pkg resp])
      (<! resp))))

(defn download
  "Download file resource"
  [url dest]
  (d/download url dest))

(defn run [c next]
  (go
    (let [r (<! c)]
      (info r ::run)
      (if (:ok r)
        (<! (next))
        r))))

(defn checksum
  "Checksum file resource"
  ([file k]
   (d/checkum file k))
  ([c file k]
   (run c (fn [] (d/checkum file k)))))

(defn summary
  "Print result"
  [c]
  (take! c
         (fn [r]
           (match r
             {:error e} (error e ::summary-fail)
             {:ok o} (info "Pipeline done" ::summary-ok)
             :else (error r ::summary-error)))))

(defn- channel?
  "check is x is a channel"
  [x]
  (= (type x) cljs.core.async.impl.channels/ManyToManyChannel))

(defn exec
  "Shell execution resource"
  [a & args]
  (if (channel? a)
    (run a (fn [] (apply sh args)))
    (apply sh (conj args a))))

(defn setup
  "Setup our environment"
  []
  (go
    (<! (load-facts))
    (info "Facts loaded" ::setup)
    (pkg-consumer (@channels :pkg))))

(defn -main [& args]
  (take! (setup)
         (fn [r]
           (info "Started re-conf" ::main)
           (println (os))
           (take! (checkum (first args) :md5) (fn [v] (info v ::log))))))

(set! *main-cli-fn* -main)

(comment
  (-> (checksum "/home/ronen/.ackrc" :md5) (exec "touch" "/tmp/bla") (summary))
  (setup)
  (os))
