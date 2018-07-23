(ns re-conf.spec.file
  "Spec checks for files"
  (:require-macros
   [clojure.core.strint :refer (<<)])
  (:require
   [re-conf.resources.common :refer (run)]
   [clojure.string :refer (includes?)]
   [cljs.core.async :as async :refer [<! go]]
   [cljs-node-io.core :as io]
   [cljs-node-io.fs :as io-fs]))

(defn check-file
  "File check spec"
  [d]
  (go
    (if-not (<! (io-fs/afile? d))
      {:error (<< "file ~{d} is missing") :exists false}
      {:ok (<< "file ~{d} exists") :exists true})))

(defn check-dir
  "Dir check spec"
  [d]
  (go
    (if-not (<! (io-fs/adir? d))
      {:error (<< "directory ~{d} is missing") :exists false}
      {:ok (<< "directory ~{d} exists") :exists true})))

(defn check-link
  "link check function"
  [src target]
  (go
    (if (<! (io-fs/asymlink? target))
      (let [[_ actual] (<! (io-fs/areadlink target))]
        (if (= actual src)
          {:ok (<< "link ~{src} to ~{target} exists") :exists true}
          {:error (<< "~{src} points to ~{actual} and not ~{target}") :exists true}))
      {:error (<< "link missing") :exists false})))

(defn contains
  "Check that a file contains string spec"
  ([f s]
   (go
     (if (includes? (io/slurp f) s)
       {:ok (<< "~{f} contains ~{s}")}
       {:error (<< "~{f} does not contain ~{s}")})))
  ([c f s]
   (run c #(contains f s))))
