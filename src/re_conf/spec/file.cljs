(ns re-conf.spec.file
  "Spec checks for files"
  (:require-macros
   [clojure.core.strint :refer (<<)])
  (:require
   [cljs.core.async :as async :refer [<! go]]
   [cljs-node-io.fs :as io-fs]))

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
          {:ok (<< "link ~{src} -> ~{target} exists") :exists true}
          {:error (<< "~{src} points to  -> ~{target} exists") :exists true}))
      {:error (<< "link missing") :exists false})))
