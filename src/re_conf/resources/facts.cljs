(ns re-conf.resources.facts
  "OS and other related system facts."
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
  (= (:status (exec-sync "bash" "-c" "type Xorg")) 0))

(defn- get- [c k]
  (fn [d]
    (let [r (js->clj d :keywordize-keys true)]
      (put! c (if k (k r) r)))))

(defn os
  "Get os information with optional sub key:

     (os) ; the entire os info map
     (os :release) ; only a single key
   "
  ([]
   (os nil))
  ([k]
   (let [c (chan)]
     (.osInfo si (get- c k))
     c)))

(defn home
  "Current user home directory"
  []
  (str (.-homedir os-)))

(defn hostname
  []
  (str (.-hostname os-)))
