(ns re-conf.resources.firewall
  "Firewall management resources"
  (:require-macros
   [cljs.core.match :refer [match]]
   [clojure.core.strint :refer (<<)])
  (:require
   [re-conf.resources.serialize :refer (call consumer)]
   [re-conf.resources.common :refer (run)]
   [re-conf.resources.log :refer (info debug error channel?)]
   [cljs.core.async :refer [<! >! go chan]]
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
  {:pre [(m :port)]}
  (match [(update m :port str)]
    [{:from f :port p}] ["from" f "to" "any" "port" p]
    [{:from f :to t :port p}] ["from" f "to" t "port" p]
    [{:port p}] [p]))

(defrecord Ufw [pipe]
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

(def firewall-pipe (chan 10))

(defn ufw []
  "Ufw provider"
  (Ufw. firewall-pipe))

(defn- into-spec [m args]
  (if (empty? args)
    m
    (let [a (first args)]
      (cond
        (map? a) (into-spec (assoc m :rule a) (rest args))
        (channel? a) (into-spec (assoc m :ch a) (rest args))
        (keyword? a) (into-spec (assoc m :state a) (rest args))
        (fn? a) (into-spec (assoc m :provider (a)) (rest args))))))

(defn rule
  "Firewall Rule resource with optional provider and state parameters:
     (rule {:port 22}) ; allow port 22
     (rule {:port 22 :from \"10.0.0.1\"}) ; port from a network
     (rule {:port 22} :absent) ; remove rule
     (rule {:port 22} :present) ; explicit present
  "
  [& args]
  (let [{:keys [ch rule state provider] :or {provider (ufw) state :present}} (into-spec {} args)
        fns {:present add :absent delete}]
    (if ch
      (run ch #(call (fns state) provider rule))
      (call (fns state) provider rule))))

(defn firewall
  "Firewall resource management
     (firewall :present) ; enable firewall
     (firewall :absent) ; disable firewall
  "
  [& args]
  (let [{:keys [ch state provider] :or {provider (ufw) state :present}} (into-spec {} args)
        fns {:present enable :absent disable}]
    (if ch
      (run ch #(call (fns state) provider))
      (call (fns state) provider))))

(defn initialize
  "Setup firewall resource serializing consumer"
  []
  (go
    (consumer firewall-pipe)))
