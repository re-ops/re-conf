(ns re-conf.resources.firewall
  "Firewall management resources"
  (:require-macros
   [cljs.core.match :refer [match]]
   [clojure.core.strint :refer (<<)])
  (:require
   [re-conf.resources.common :refer (run)]
   [re-conf.resources.log :refer (info debug error channel?)]
   [cljs.core.async :refer [<! >! go]]
   [re-conf.resources.facts :refer (os)]
   [re-conf.resources.shell :refer (sh)]))

(defprotocol Firewall
  (enable [this])
  (disable [this])
  (add [this rule])
  (delete [this rule]))

(def ufw-bin
  "/usr/sbin/ufw")

(defn- ufw-rule
  "ufw add rule syntax from map"
  [m]
  (match [m]
    [{:from f :port p}] ["from" f "to" "any" "port" p]
    [{:from f :to t :port p}] ["from" f "to" t "port" p]
    [{:port p}] [p]))

(defrecord Ufw []
  Firewall
  (enable [this]
    (debug "enable ufw" ::ufw)
    (apply sh [ufw-bin "--force" "enable"]))

  (disable [this]
    (debug "disable ufw" ::ufw)
    (apply sh [ufw-bin "--force" "disable"]))

  (add [this rule]
    (debug (<< "add rule ~{rule}") ::ufw)
    (apply sh (into [ufw-bin "allow"] (ufw-rule rule))))

  (delete [this rule]
    (debug (<< "delete rule ~{rule}") ::ufw)
    (apply sh (into [ufw-bin "delete"] (ufw-rule rule)))))

(defn ufw []
  "Ufw provider"
  (Ufw.))

(defn- into-spec [m args]
  (if (empty? args)
    m
    (let [a (first args)]
      (cond
        (map? a) (into-spec (clojure.core/update m :rule (fn [v] (conj v a))) (rest args))
        (channel? a) (into-spec (assoc m :ch a) (rest args))
        (keyword? a) (into-spec (assoc m :state a) (rest args))
        (fn? a) (into-spec (assoc m :provider (a)) (rest args))))))

(defn rule
  "Firewall Rule resource with optional provider and state parameters:
     (rule {:port 22}) ; allow port 22
     (rule {:port 22 :from \"10.0.0.1\"}) ; port from a network
     (rule \"22\" :present) ; explicit state
     (rule \"22\" :absent) ; remove rule
  "
  ([& args]
   (let [{:keys [ch rule state provider] :or {provider (ufw) state :present}} (into-spec {} args)
         fns {:present add :absent delete}]
     (if ch
       (run ch #((fns state) provider rule))
       ((fns state) provider rule)))))

