#! /usr/bin/env hy
(import hy)

(eval (hy.lex.tokenize (.read (open "src/shim/basic.clj" "r"))))

(defmacro ns [n rs])

(defn println [s]
  (print s))

(defmain [&rest args]
  (install-tmux))

