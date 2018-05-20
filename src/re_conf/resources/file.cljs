(ns re-conf.resources.file
  "File resources"
  (:require-macros
   [clojure.core.strint :refer (<<)])
  (:require
   [clojure.string :refer (includes?)]
   [re-conf.resources.log :refer (info debug error)]
   [re-conf.resources.common :refer (run obj->clj)]
   [re-conf.resources.shell :refer (sh)]
   [re-conf.resources.log :refer (info)]
   [re-conf.spec.file :refer (check-link check-dir)]
   [cljs.core.async :as async :refer [<! go put! chan]]
   [cljs-node-io.core :as io]
   [cljs-node-io.fs :as io-fs]
   [cljstache.core :refer [render]]))

(def fs (js/require "fs"))

; utility functions

(defn- translate
  "convert io.fs nil to :ok and err? into {:error e}"
  [c m]
  (go
    (let [r (<! c)]
      (cond
        (nil? (first r)) {:ok m}
        :else {:error (map obj->clj r)}))))

(defn stats
  "Get file stats info"
  [dest]
  (go
    (let [[err stat] (<! (io-fs/astat dest))
          prms (io-fs/permissions stat)]
      (if (nil? err)
        {:ok (assoc (obj->clj stat) :mode prms)}
        {:error err}))))

; inner resource implemetation

(defn- run-template
  "Create a file from a template with args"
  [args tmpl dest]
  (go
    (let [[esl s] (<! (io/aslurp tmpl))]
      (if esl
        {:error {:message "reading template source failed" :error esl :source tmpl}}
        (let [[esp] (<! (io/aspit dest (render s args)))]
          (if esp
            {:error {:message "reading template source failed" :error esp :source tmpl}}
            {:ok {:message "writing template source success" :template tmpl :dest dest}}))))))

(defn- options [args opts]
  (cond->> args
    (opts :recursive) (into args ["-R"])))

(defn template
  "File template resource"
  ([args tmpl dest]
   (template nil args tmpl dest))
  ([c args tmpl dest]
   (run c (fn [] (run-template args tmpl dest)))))

(defn chown
  "Change file/directory owner resource"
  ([dest uid gid]
   (translate (io-fs/achown dest uid gid) (<< "~{dest} uid:gid is set to ~{uid}:~{gid}")))
  ([c dest uid gid]
   (run c #(chown dest uid gid))))

(defn chmod
  "Change file/directory mode resource"
  ([dest mode]
   (translate (io-fs/achmod dest mode) (<< "~{dest} mode is set to ~{mode}")))
  ([c dest mode]
   (run c #(chmod dest mode))))

(defn rmdir [d]
  (go
    (if-not (:exists (<! (check-dir d)))
      [nil (<< "folder ~{d} missing, skipping rmdir")]
      (let [[err v]  (<! (io-fs/areaddir d))]
        (if err
          [err]
          (if (empty? v)
            (<! (io-fs/armdir d))
            (<! (io-fs/arm-r d))))))))

(defn mkdir [d]
  (go
    (if-not (:exists (<! (check-dir d)))
      (<! (io-fs/amkdir d))
      [nil (<< "folder ~{d} exists, skipping mkdir")])))

(def directory-states {:present mkdir
                       :absent rmdir})

(defn directory
  "Directory resource"
  ([dest]
   (directory nil dest :present))
  ([c dest]
   (directory c dest :present))
  ([c dest state]
   (run c #(translate ((directory-states state) dest) (<< "Directory ~{dest} is ~(name state)")))))

(defn mklink
  [src target]
  (go
    (let [{:keys [error ok exists] :as m} (<! (check-link src target))]
      (if-not exists
        (<! (io-fs/asymlink src target))
        (if ok
          [nil ok]
          [error])))))

(def symlink-states {:present mklink})

(defn symlink
  "Symlink resource"
  ([src target]
   (symlink src target :present))
  ([src target state]
   (translate
    ((symlink-states state) src target)
    (<< "Symlink from ~{src} to ~{target} is ~(name state)")))
  ([c src target state]
   (run c #(symlink src target state))))

(defn contains
  "Check that a file contains string spec"
  ([f s]
   (go
     (if (includes? (io/slurp f) s)
       {:ok (<< "~{f} contains ~{s}")}
       {:error (<< "~{f} does not contain ~{s}")})))
  ([c f s]
   (run c #(contains f s))))

(comment
  (info (io-fs/areadlink "/home/re-ops/.tmux.conf") ::symlink)
  (info (chmod "/tmp/fo" "0777") ::chmod)
  (sh "/bin/chmod" mode dest :sudo true :dry true))
