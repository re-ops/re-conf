(ns re-conf.resources.facts
  (:require
   [re-conf.resources.shell :refer (exec-sync)]
   [re-conf.resources.log :refer (error info)]
   [cljs.core.async :as async :refer [<! put! chan]]))

(def si (js/require "systeminformation"))
(def os- (js/require "os"))
(def process (js/require "process"))

(defn desktop?
  "Are we running in a Linux desktop?"
  []
  (= (:status (exec-sync "type" "Xorg")) 0))

(defn get- [c k]
  (fn [d]
    (let [r (js->clj d :keywordize-keys true)]
      (put! c (if k (k r) r)))))

(defn os
  ([]
   (os nil))
  ([k]
   (let [c (chan)]
     (.osInfo si (get- c k))
     c)))

(defn home
  "current user home directory"
  []
  (str (.-homedir os-)))
