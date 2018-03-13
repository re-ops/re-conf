(ns shim.core
  (:require [cljs.nodejs :as nodejs]))

(nodejs/enable-util-print!)

(defn hey [] (println "heyy"))

(defn -main []
  (println "Hello world!"))

(set! *main-cli-fn* -main)
