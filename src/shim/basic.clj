(ns shim.basic
  (:require [shim.cljs.core :refer (install pretty)]))

(defn install-tmux []
  (-> (install "tmux") (pretty "done installing tmux")))
