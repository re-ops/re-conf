(ns re-conf.cljs.facts
  (:require
   [re-conf.cljs.log :refer (error info)]
   [cljs.core.async :as async :refer [<! put! chan]]))

(def si (js/require "systeminformation"))

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

(comment
  (info (os) ::log))
