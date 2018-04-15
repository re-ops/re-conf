(ns re-conf.cljs.facts
  (:require
   [re-conf.cljs.log :refer (error info)]
   [cljs.core.async :as async :refer [<! put! chan]]))

(def si (js/require "systeminformation"))
(def os- (js/require "os"))

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

(comment
  (home)
  (info (os) ::log))
