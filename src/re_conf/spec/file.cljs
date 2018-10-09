(ns re-conf.spec.file
  "Spec checks for files"
  (:require-macros
   [clojure.core.strint :refer (<<)])
  (:require
   [cljs.spec.alpha :as s]
   [re-conf.resources.common :refer (run error? obj->clj)]
   [clojure.string :refer (includes?)]
   [cljs.core.async :as async :refer [<! go]]
   [cljs-node-io.core :as io]
   [cljs-node-io.fs :as io-fs]))

; specs

(s/def mode string?)

(s/def file string?)

; check functions
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
     (if-let [e (error? (<! (check-file f)))]
       {:error e}
       (if (includes? (io/slurp f) s)
         {:ok (<< "~{f} contains ~{s}") :present true}
         {:error (<< "~{f} does not contain ~{s}") :present false}))))

  ([c f s]
   (run c #(contains f s))))

(defn stats
  "Get file stats info"
  [dest]
  (go
    (let [[err stat] (<! (io-fs/astat dest))
          prms (io-fs/permissions stat)]
      (if (nil? err)
        {:ok (assoc (obj->clj stat) :mode prms)}
        {:error err}))))

