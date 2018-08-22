(ns re-conf.resources.serialize
  "Support for resources that can't support concurrent access (like package managers/firewalls etc.."
  (:require
   [re-conf.resources.log :refer (info debug error channel?)]
   [cljs.core.async :refer [<! >! go go-loop chan]]))

(defn call [f provider & args]
  (go
    (let [resp (chan)]
      (>! (:pipe provider) [f provider args resp])
      (<! resp))))

(defn consumer [c]
  (go-loop []
    (let [[f provider args resp] (<! c)
          result (<! (apply f provider args))]
      (>! resp result))
    (recur)))
