(ns re-conf.rcp.shell
  "Shell setup recipes"
  (:require-macros
   [clojure.core.strint :refer (<<)])
  (:require
   [re-conf.cljs.pkg :refer (install)]
   [re-conf.cljs.facts :refer (home)]
   [re-conf.cljs.git :refer (clone)]
   [re-conf.cljs.shell :refer (exec)]
   [re-conf.cljs.core :refer (summary)]))

(defn tmux
  "Setup tmux"
  []
  (->
   (install "tmux")
   (clone "git://github.com/narkisr/.tmux.git" (<< "~(home)/.tmux"))))
