(ns re-conf.cljs.file
  "File resources"
  (:require-macros
   [clojure.core.strint :refer (<<)])
  (:require
   [cljs-node-io.fs :refer (amkdir)]
   [re-conf.cljs.common :refer (run obj->clj)]
   [re-conf.cljs.shell :refer (sh)]
   [re-conf.cljs.log :refer (info)]
   [cljs.core.async :as async :refer [<! go put! chan]]
   [cljs-node-io.core :as io]
   [cljstache.core :refer [render]]))

(def fs (js/require "fs"))

(defn translate
  "convert io.fs nil to :ok and err? into {:error e}"
  [c m]
  (go
    (let [r (<! c)]
      (cond
        (nil? r) {:ok m}
        :else {:error (map obj->clj r)}))))

; utility functions
(defn exists?
  "Check if a file exists"
  [dest]
  (.existsSync fs dest))

(defn stats
  "Get file stats info"
  [dest]
  (let [c (chan)]
    (.stat fs dest
           (fn [e s]
             (if e
               (put! c {:error e})
               (put! c {:ok (js->clj s)}))))
    c))

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

(defn- run-chown
  "Change file/folder owner and group"
  [dest usr grp & options]
  (sh "/bin/chown" (<< "~{usr}:~{grp}") dest :sudo true))

(defn- run-chmod
  "Change change file/folder owner and group"
  [dest mode & options]
  (sh "/bin/chmod" mode dest))

(defn run-symlink
  "Create a symlink between source and target"
  [src target]
  (let [c (chan)]
    (.symlink fs src target
              (fn [e]
                (if e
                  (put! c {:error e})
                  (put! c {:ok (<< "created symlink from ~{src} to ~{target}")}))))
    c))

(defn template
  "File template resource"
  ([args tmpl dest]
   (template nil args tmpl dest))
  ([c args tmpl dest]
   (run c (fn [] (run-template args tmpl dest)))))

(defn chown
  "Change file/directory owner resource"
  ([dest usr grp]
   (run-chown dest usr grp))
  ([c dest usr grp]
   (run c #(run-chown dest usr grp))))

(defn chmod
  "Change file/directory mode resource"
  ([dest mode]
   (run-chmod dest mode))
  ([c dest mode]
   (run c #(run-chmod dest mode))))

(defn directory
  "Directory creation resource"
  ([dest]
   (translate (amkdir dest) (<< "directory ~{dest} created")))
  ([c dest]
   (run c #(directory dest))))

(defn symlink
  "Symlink resource"
  ([src target]
   (run-symlink src target))
  ([c src target]
   (run c #(run-symlink src target))))

(comment
  (info (directory "/tmp/5") ::directory)
  (info (chmod "foo" "+x") ::chmod)
  (sh "/bin/chmod" mode dest :sudo true :dry true))
