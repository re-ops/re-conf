(ns re-conf.resources.mount
  "Mount resources"
  (:require-macros
   [clojure.core.strint :refer (<<)])
  (:require
   [re-conf.resources.log :refer (info debug error channel?)]
   [re-conf.resources.common :refer (run obj->clj error?)]
   [re-conf.resources.file :refer (line)]
   [re-conf.resources.shell :refer (sh)]
   [cljs.core.async :as async :refer [<! go put! chan]]))

(def bin-mount "/bin/mount")

(def bin-unmount "/bin/unmount")

(defn- persist
  "Add mount point to fstab"
  [device target {:keys [type options dump pass]}]
  (let [mount-line (<< "~{device} ~{target} ~{type} ~{options} ~{dump} ~{pass}")]
    (go
      (if-let [e (error? (<! (line "/etc/fstab" mount-line :present)))]
        {:error e}
        (<! (sh bin-mount "-a"))))))

(defn- run-mount
  [device target {:keys [persist] :as options}]
  (go
    (if persist
      (<! (persist device target options))
      (<! (sh bin-mount device target)))))

(defn- run-unmount
  [target options]
  (go
    (<! (sh bin-unmount target))))

(def mount-states
  {:present run-mount :absent run-unmount})

(defn mount
  "Mount resource"
  ([device target state options]
   (mount nil target state options))
  ([c device target state options]
   (run c (mount-states state) [device target options])))
