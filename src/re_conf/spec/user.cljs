(ns re-conf.spec.user
  "Specification for users"
  (:require
   [cljs.core.async :refer [<! go]]
   [re-conf.resources.shell :refer (sh)]))

(defn user-exists?
  "checks if a user exists"
  [user]
  (go
    (<! (sh "/usr/bin/id" "-u" user))))

(defn group-exists?
  "checks if a group exists"
  [group]
  (go
    (<! (sh "/usr/bin/getent" "group" group))))
