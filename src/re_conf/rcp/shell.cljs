(ns re-conf.rcp.shell
  "Shell setup recipes"
  (:require-macros
   [clojure.core.strint :refer (<<)])
  (:require
   [re-conf.cljs.pkg :refer (package)]
   [re-conf.cljs.file :refer (chown directory symlink)]
   [re-conf.cljs.facts :refer (home)]
   [re-conf.cljs.git :refer (clone)]
   [re-conf.cljs.shell :refer (exec)]
   [re-conf.cljs.output :refer (summary)]))

(defn ^{:private true} tmux
  "Setup tmux for user"
  [{:keys [user]}]
  (let [home (<< "/home/~{user}/") dest (<< "~{home}/.tmux")]
    (->
     (package "tmux")
     (clone "git://github.com/narkisr/.tmux.git")
     (chown user dest)
     (directory (<< "~{dest}/plugins") :present)
     (clone "git://github.com/tmux-plugins/tpm" (<< "~{dest}/plugins/tpm"))
     (symlink (<< "{dest}/.tmux.conf") (<< "~{home}/.tmux.conf")))))

(defn tmuxinator
  "Setup Tmuxinator"
  [{:keys [user]}])

(comment)
