(ns re-conf.cljs.basic
  (:require [re-conf.cljs.core :refer (install pretty)]))

(defn install-tmux []
  (-> (install "tmux") (pretty "done installing tmux")))

(comment
  (install-tmux))
