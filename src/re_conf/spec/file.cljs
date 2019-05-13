(ns re-conf.spec.file
  "Spec checks for files"
  (:require-macros
   [clojure.core.strint :refer (<<)])
  (:require
   [re-conf.spec.common :refer (valid?)]
   [cljs.spec.alpha :as s]
   [re-conf.resources.common :refer (run error? obj->clj)]
   [clojure.string :refer (includes?)]
   [cljs.core.async :as async :refer [<! go]]
   [cljs-node-io.core :as io]
   [cljs-node-io.fs :as io-fs]))

; specs

(s/def ::path (s/and string? #(re-matches #"[^\\0]+" %)))

; check functions

(defn check-file
  "Check that a file exists"
  [path]
  {:pre [(valid? ::path path)]}
  (go
    (if-not (<! (io-fs/afile? path))
      {:error (<< "file ~{path} is missing") :exists false}
      {:ok (<< "file ~{path} exists") :exists true})))

(defn check-dir
  "Check that a directory exists"
  [directory]
  {:pre [(valid? ::path directory)]}
  (go
    (if-not (<! (io-fs/adir? directory))
      {:error (<< "directory ~{directory} is missing") :exists false}
      {:ok (<< "directory ~{directory} exists") :exists true})))

(defn check-link
  "Link check function:

   (check-link \"/tmp/foo\") ; check a link exists
   (check-link \"/tmp/foo\" \"/tmp/bar\") ; check that a link exists and points to target"
  ([path]
   {:pre [(valid? ::path path)]}
   (go
     (if (<! (io-fs/asymlink? path))
       {:ok (<< "link ~{path} exists") :exists true}
       {:error (<< "link missing") :exists false})))
  ([src target]
   {:pre [(valid? ::path src) (valid? ::path target)]}
   (go
     (if (<! (io-fs/asymlink? target))
       (let [[_ actual] (<! (io-fs/areadlink target))]
         (if (= actual src)
           {:ok (<< "link ~{src} to ~{target} exists") :exists true}
           {:error (<< "~{src} points to ~{actual} and not ~{target}") :exists true}))
       {:error (<< "link missing") :exists false}))))

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
   (run c contains [f s])))

(defn stats
  "Get file stats info"
  [dest]
  {:pre [(valid? ::path dest)]}
  (go
    (let [[err stat] (<! (io-fs/astat dest))
          prms (io-fs/permissions stat)]
      (if (nil? err)
        {:ok (assoc (obj->clj stat) :mode prms)}
        {:error err}))))
