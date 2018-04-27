(ns re-conf.test.pkg
  (:require
   [cljs.core.async :refer [<! go]]
   [re-conf.cljs.pkg :refer (package installed? initialize)]
   [cljs.test :refer-macros  [deftest is testing async]]))

(defn ok?  [m]
  (contains? m :ok))

(deftest packaging
  (async done
         (go
           (let [_ (initialize)
                 present (<! (package "gt5"))
                 installed (<! (installed? "gt5"))
                 absent (<! (package "gt5" :absent))]
             (is (ok? present))
             (is (ok? installed))
             (is (ok? absent))
             (done)))))
