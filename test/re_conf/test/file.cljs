(ns re-conf.test.file
  (:require
    [cljs-node-io.fs :refer (dir?)]
    [cljs.core.async :as async :refer [<! go]]
    [re-conf.cljs.file :refer (chown directory exists?)]
    [cljs.test :refer-macros  [deftest is testing async]]))

(def fs (js/require "fs"))

(deftest resources
  (testing "directory creation"
    (async done
      (go 
        (is (:ok (<! (directory "/tmp/2" 777)))))
        (is (dir? "/tmp/2"))
        (done)) 
    )
  )
