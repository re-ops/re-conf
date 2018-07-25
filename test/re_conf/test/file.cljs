(ns re-conf.test.file
  (:require
   [cljs-node-io.fs :refer (adir?)]
   [cljs.core.async :as async :refer [<! go]]
   [re-conf.resources.file :refer (chown chmod directory stats line file)]
   [cljs.test :refer-macros  [deftest is testing async]]))

(def fs (js/require "fs"))

(defn ok?  [m]
  (contains? m :ok))

(deftest directory-modes
  (async done
         (go
           (let [present (<! (directory "/tmp/2" :present))
                 mode (<! (chmod "/tmp/2" 0777))
                 stat (:ok (<! (stats "/tmp/2")))
                 dir (<! (adir? "/tmp/2"))
                 absent (<! (directory "/tmp/2" :absent))]
             (is (ok? present))
             (is (ok? absent))
             (is (ok? mode))
             (is (= 511 (stat :mode)))
             (is dir)
             (done)))))

(deftest line-manipulation
  (async done
         (go
           (let [present (<! (file "/tmp/3" :present))
                 append (<! (line "/tmp/3" "key = value" :present))
                 set-key (<! (line  "/tmp/3" "key" "foo" " = " :set))
                 absent (<! (file "/tmp/3" :absent))]
             (is (ok? present))
             (is (ok? append))
             (is (ok? set-key))
             (is (ok? absent))
             (done)))))
