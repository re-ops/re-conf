(ns re-conf.test.suite
  (:require
   re-conf.test.file
   re-conf.test.pkg
   [cljs.nodejs :as nodejs]
   [cljs.test :refer-macros  [run-tests]]))

(nodejs/enable-util-print!)

(defn sudoless
  "Tests that don't require sudo"
  []
  (run-tests
   're-conf.test.file))

(defn all []
  (run-tests
   're-conf.test.pkg
   're-conf.test.file))

(all)
;; (sudoless)
