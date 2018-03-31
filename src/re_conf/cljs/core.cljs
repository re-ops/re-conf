(ns re-conf.cljs.core
  (:require
   [re-conf.cljs.facts :refer (load-facts os)]
   [re-conf.cljs.shell :refer (sh)]
   [re-conf.cljs.log :refer (info debug)]
   [re-conf.cljs.download :as d]
   [fipp.edn :refer (pprint)]
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
  ([pkg]
   (install nil pkg))
  ([res pkg]
   (go
     (let [resp (chan)]
       (>! (@channels :pkg) [res pkg resp])
       (<! resp)))))

(defn download
  ([url dest]
   (download nil url dest))
  ([res url dest]
   (d/download url dest)))

(defn checkum
  ([file k]
   (checkum nil file k))
  ([res file k]
   (d/checkum file k)))

(defn pretty [res s]
  (info res ::log))

(defn setup
  "Setup our environment"
  []
  (go
    (<! (load-facts))
    (println "facts loaded")
    (pkg-consumer (@channels :pkg))))

(defn -main [& args]
  (take! (setup)
         (fn [r]
           (println "started re-conf")
           (println (os))
           (take! (checkum (first args) :md5) (fn [v] (info v ::log))))))

(set! *main-cli-fn* -main)

(comment
  (setup)
  (os))
