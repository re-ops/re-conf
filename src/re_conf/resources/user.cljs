(ns re-conf.resources.user
  "User managment"
  (:refer-clojure :exclude [name])
  (:require-macros
   [clojure.core.strint :refer (<<)])
  (:require
   [re-conf.resources.log :refer (info debug error channel?)]
   [re-conf.resources.common :refer (run)]
   [cljs.core.async :refer [<! go take!]]
   [re-conf.resources.shell :refer (sh)]))

(defn append-options
  [args opts]
  (cond-> args
    (opts :home) (into ["--home" (<< "/home/~(second args)")])))

(defn adduser
  "Add a user"
  [name options]
  (let [props "'First Last,RoomNumber,WorkPhone,HomePhone'"]
    (go
      (<! (apply sh (append-options ["/usr/sbin/adduser" name "--gecos" props "--disabled-password"] options))))))

(defn rmuser
  "Remove a user"
  [name & _]
  (go
    (<! (sh "/usr/sbin/deluser" "--remove-home" name))))

(def user-states {:present adduser
                  :absent rmuser})

(defn user
  "User management"
  ([name]
   (user nil name {} :present))
  ([name state]
   (user nil name state {}))
  ([name state options]
   ((user-states state) name options))
  ([c name state options]
   (run c #(user name state options))))

(comment
  (info (user "zfs" :present {:home true}) :zfs)
  (info (user "zfs" :absent) :zfs))
