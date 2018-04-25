(ns re-conf.test.file
  (:require
   [cljs-node-io.fs :refer (adir?)]
   [cljs.core.async :as async :refer [<! go]]
   [re-conf.cljs.file :refer (chown directory)]
   [cljs.test :refer-macros  [deftest is testing async]]))

(def fs (js/require "fs"))

(defn ok?  [m]
  (contains? m :ok))

(deftest resources
  (testing "directory resource"
    (async done
           (go
             (let [present (<! (directory "/tmp/2" :present))
                   dir (<! (adir? "/tmp/2"))
                   absent (<! (directory "/tmp/2" :absent))]
               (is (ok? present))
               (is dir)
               (is (ok? absent))
               (done))))))
