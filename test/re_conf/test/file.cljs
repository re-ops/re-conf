(ns re-conf.test.file
  (:require
   [cljs-node-io.fs :refer (adir?)]
   [cljs.core.async :as async :refer [<! go]]
   [re-conf.cljs.file :refer (chown directory)]
   [cljs.test :refer-macros  [deftest is testing async]]))

(def fs (js/require "fs"))

(deftest resources
  (testing "directory creation"
    (async done
           (go
             (is (:ok (<! (directory "/tmp/2"))))
             (is (nil? (<! (adir? "/tmp/2")))))
           (done))))
