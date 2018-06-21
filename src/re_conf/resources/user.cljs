(ns re-conf.resources.user
  "User managment"
  (:refer-clojure :exclude [name])
  (:require-macros
   [clojure.core.strint :refer (<<)])
  (:require
   [re-conf.resources.log :refer (info debug error channel?)]
   [re-conf.resources.common :refer (run)]
   [cljs.core.async :refer [<! go]]
   [re-conf.resources.shell :refer (sh)]))

(defn adduser
  "Add a user"
  [name]
  (let [props "'First Last,RoomNumber,WorkPhone,HomePhone'"]
    (go
      (<! (sh "/usr/sbin/adduser" name "--gecos" props  "--disabled-password")))))

(defn rmuser
  "Remove a user"
  [name]
  (go
    (<! (sh "/usr/sbin/deluser" "" name))))

(def user-states {:present adduser
                  :absent rmuser})

(defn user
  "User management"
  ([name]
   (user nil name :present))
  ([name state]
   (user nil name state))
  ([c name state]
   (run c #((user-states state) name))))

(comment
  (info (user "zfs") :zfs)
  (info (user "zfs" :absent) :zfs))
