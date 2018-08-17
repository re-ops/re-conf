(ns re-conf.resources.user
  "User managment"
  (:refer-clojure :exclude [name])
  (:require-macros
   [clojure.core.strint :refer (<<)])
  (:require
   [re-conf.spec.user :refer (user-exists? group-exists?)]
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

(defn addgroup
  "Add a user"
  [name options]
  (go
    (let [{:keys [ok]} (<! (group-exists? name))]
      (if-not ok
        (<! (apply sh (append-options ["/usr/sbin/addgroup" name] options)))
        {:ok "group already exists skipping creation"}))))

(defn rmgroup
  "Remove a group"
  [name & _]
  (go
    (<! (sh "/usr/sbin/delgroup" name))))

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
  "User resource"
  ([name]
   (user nil name {} :present))
  ([name state]
   (user nil name state {}))
  ([name state options]
   ((user-states state) name options))
  ([c name state options]
   (run c #(user name state options))))

(def group-states {:present addgroup
                   :absent rmgroup})

(defn group
  "Group resource"
  ([name]
   (group nil name {} :present))
  ([name state]
   (group nil name state {}))
  ([name state options]
   ((group-states state) name options))
  ([c name state options]
   (run c #(group name state options))))
