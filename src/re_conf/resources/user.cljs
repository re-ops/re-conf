(ns re-conf.resources.user
  "User managment"
  (:refer-clojure :exclude [name])
  (:require-macros
   [clojure.core.strint :refer (<<)])
  (:require
   [re-conf.spec.user :refer (user-exists?)]
   [re-conf.resources.log :refer (info debug error channel?)]
   [re-conf.resources.common :refer (run)]
   [cljs.core.async :refer [<! go take!]]
   [re-conf.resources.shell :refer (sh)]))

(defn append-options
  [args opts]
  (cond-> args
    (not (opts :home)) (into ["--no-create-home"])
    (opts :gid) (into ["--gid" (opts :gid)])
    (opts :uid) (into ["--uid" (opts :uid)])
    (opts :home) (into ["--home" (<< "/home/~(second args)")])))

(defn adduser
  "Add a user"
  [name options]
  (let [props "'First Last,RoomNumber,WorkPhone,HomePhone'"]
    (go
      (let [{:keys [ok]} (<! (user-exists? name))]
        (if-not ok
          (<! (apply sh (append-options ["/usr/sbin/adduser" name "--gecos" props "--disabled-password"] options)))
          {:ok "user already exists skipping creation"})))))

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
