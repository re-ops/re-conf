(ns re-conf.cljs.file
  "File resources"
  (:require-macros
   [clojure.core.strint :refer (<<)])
  (:require
   [cljs-node-io.fs :as fs]
   [re-conf.cljs.common :refer (run obj->clj)]
   [re-conf.cljs.shell :refer (sh)]
   [re-conf.cljs.log :refer (info)]
   [cljs.core.async :as async :refer [<! go put! chan]]
   [cljs-node-io.core :as io]
   [cljstache.core :refer [render]]))

(def nfs (js/require "fs"))

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
    (let [[err stat] (<! (fs/astat dest))
          prms (fs/permissions stat)]
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
   (translate (fs/achown dest uid gid) (<< "~{dest} uid:gid is set to ~{uid}:~{gid}")))
  ([c dest uid gid]
   (run c #(chown dest uid gid))))

(defn chmod
  "Change file/directory mode resource"
  ([dest mode]
   (translate (fs/achmod dest mode) (<< "~{dest} mode is set to ~{mode}")))
  ([c dest mode]
   (run c #(chmod dest mode))))

(defn rmdir [d]
  (go
    (let [[err v]  (<! (fs/areaddir d))]
      (if err
        [err]
        (if (empty? v)
          (<! (fs/armdir d))
          (<! (fs/arm-r d)))))))

(def directory-states {:present fs/amkdir
                       :absent rmdir})

(defn directory
  "Directory resource"
  ([dest]
   (directory dest :present))
  ([dest state]
   (translate ((directory-states state) dest) (<< "Directory ~{dest} state is ~(name state)")))
  ([c dest state]
   (run c #(directory dest state))))

(def symlink-states {:present fs/asymlink
                     :absent fs/arm})

(defn symlink
  "Symlink resource"
  ([src target]
   (symlink src target :present))
  ([src target state]
   (translate ((symlink-states state) src target)
              (<< "Symlink from ~{src} to ~{target} is ~(name state)")))
  ([c src target state]
   (run c #(symlink src target state))))

(comment
  (info (stats "/tmp/fo") ::stats)
  (info (directory "/tmp/fo" :present) ::chmod)
  (info (symlink "/tmp/fo" "/tmp/bla") ::symlink)
  (info (chmod "/tmp/fo" "0777") ::chmod)
  (sh "/bin/chmod" mode dest :sudo true :dry true))
