(ns re-conf.resources.service
  "Service handling"
  (:require-macros
   [clojure.core.strint :refer (<<)])
  (:require
   [re-conf.resources.common :refer (run)]
   [re-conf.resources.facts :refer (os)]
   [cljs.core.async :refer [<! go]]
   [re-conf.resources.log :refer (info debug error channel?)]
   [re-conf.resources.shell :refer (sh)]))

(defprotocol Service
  (stop [this service])
  (start [this service])
  (restart [this service])
  (disable [this service])
  (enable [this service]))

(def systemd-bin "/usr/sbin/service")

(defn sysctl []
  (go
    (case (<! (os :release))
      "16.04" "/sbin/systemctl"
      "18.04" "/bin/systemctl"
      :no-matching-release!)))

(defrecord Systemd []
  Service
  (start [this service]
    (debug "starting service" ::systemd)
    (go
      (<! (sh systemd-bin service "start"))))

  (stop [this service]
    (debug "stopping service" ::systemd)
    (go
      (<! (sh systemd-bin service "stop"))))

  (restart [this service]
    (debug "restarting service" ::systemd)
    (go
      (<! (sh systemd-bin service "restart"))))

  (disable [this service]
    (debug "disabling service" ::systemd)
    (go
      (<! (sh (<! (sysctl)) "disable" (<< "~{service}.service")))))

  (enable [this service]
    (debug "enabling service" ::systemd)
    (go
      (<! (sh (<! (sysctl)) "enable" (<< "~{service}.service"))))))

(def systemd (Systemd.))

(defn into-spec [m args]
  (if (empty? args)
    m
    (let [a (first args)]
      (cond
        (string? a) (into-spec (assoc m :name a) (rest args))
        (channel? a) (into-spec (assoc m :ch a) (rest args))
        (keyword? a) (into-spec (assoc m :state a) (rest args))
        (fn? a) (into-spec (assoc m :provider (a)) (rest args))))))

(defn service
  "Service resource with optional provider and state parameters"
  ([& args]
   (let [{:keys [ch name state provider] :or {provider systemd state :restart}} (into-spec {} args)
         fns {:start start :stop stop :disable disable :enable enable :restart restart}]
     (run ch (fns state) [provider name]))))

