(ns re-conf.test.suite
  (:require
   re-conf.test.file
   [cljs.nodejs :as nodejs]
   [cljs.test :refer-macros  [run-tests]]))

(nodejs/enable-util-print!)

(run-tests 're-conf.test.file)
