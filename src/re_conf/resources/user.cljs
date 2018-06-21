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

(defn append-options
   [args opts]
  (cond->> args
    (opts :home) (into args ["-m"])))

(defn adduser
  "Add a user"
  [name options]
  (let [props "'First Last,RoomNumber,WorkPhone,HomePhone'"]
    (go
      (<!
        (apply sh
           (append-options ["/usr/sbin/adduser" name "--gecos" props  "--disabled-password"] options))))))

(defn rmuser
  "Remove a user"
  [name]
  (go
    (<! (sh "/usr/sbin/deluser" "" name))))

(def user-states {:present adduser
                  :absent rmuser})

(defn user
  "User management"
  ([name options]
   (user nil name :present))
  ([name options state]
   (user nil name options state))
  ([c name options state]
   (run c #((user-states state) name options))))

(comment
  (info (user "zfs") :zfs)
  (info (user "zfs" :absent) :zfs))
