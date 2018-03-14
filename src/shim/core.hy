#! /usr/bin/env hy
(require [hy.contrib.multi [defn]])
(require [hy.contrib.walk [let]])
(import hy)
(import pprint)
(import subprocess)

(defmacro ns [n rs])

(defn file
  [res src]
  {:result :ok})

(defn facts []
  (subprocess.check-call "facter" "--json"))

(defn install
  ([p] (install None p))
  ([res p]
    (subprocess.check-call "" "-la")
     {:result :ok}))

(defn pretty [res s]
  (let [pp (pprint.PrettyPrinter :indent 4)]
    (.pprint pp res)))

(defn println [s]
  (print s))


(eval (read-str (.read (open "src/shim/basic.clj" "r"))))

(defmain [&rest args]
  (install-tmux))

