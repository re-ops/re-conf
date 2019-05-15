(ns re-conf.test.file
  (:require
   [cljs-node-io.fs :refer (adir?)]
   [cljs.core.async :as async :refer [<! go]]
   [re-conf.resources.file :refer (symlink chown chmod directory line file)]
   [re-conf.resources.common :refer (ok?)]
   [re-conf.spec.file :refer (stats check-link check-dir contains)]
   [cljs.test :refer-macros  [deftest is testing async]]))

(def fs (js/require "fs"))

(deftest directory-modes
  (async done
         (go
           (let [present (<! (directory "/tmp/2" :present))
                 mode (<! (chmod "/tmp/2" "0777"))
                 stat (:ok (<! (stats "/tmp/2")))
                 was-added (<! (check-dir "/tmp/2"))
                 absent (<! (directory "/tmp/2" :absent))
                 was-removed (check-dir "/tmp/2")]
             (is (ok? present))
             (is (ok? mode))
             (is (= 511 (stat :mode)))
             (is (:exists was-added))
             (is (ok? absent))
             (is (not (:exists was-removed)))
             (done)))))

(deftest line-manipulation
  (async done
         (go
           (let [present (<! (file "/tmp/3" :present))
                 appended (<! (line "/tmp/3" "key = value" :present))
                 line-added (<! (contains "/tmp/3" "key = value"))
                 set-key (<! (line  "/tmp/3" "key" "foo" " = " :set))
                 absent (<! (file "/tmp/3" :absent))]
             (is (ok? present))
             (is (ok? appended))
             (is (:exists line-added))
             (is (ok? set-key))
             (is (ok? absent))
             (done)))))

(deftest missing-file
  (async done
         (go
           (let [set-key (<! (line  "/tmp/2" "key" "foo" " = " :set))
                 append (<! (line "/tmp/3" "key = value" :present))
                 absent (<! (file "/tmp/2" :absent))]
             (is (not (ok? set-key)))
             (is (not (ok? append)))
             (is (ok? absent))
             (done)))))

(deftest linking
  (async done
         (go
           (let [touch (<! (file "/tmp/source" :present))
                 present (<! (symlink "/tmp/source" "/tmp/target"))
                 was-added (<! (check-link "/tmp/target"))
                 absent (<! (symlink "/tmp/target" :absent))
                 was-removed (<! (check-link "/tmp/target"))]
             (is (ok? touch))
             (is (ok? present))
             (is (was-added :exists))
             (is (ok? absent))
             (is (not (was-removed :exists)))
             (done)))))


