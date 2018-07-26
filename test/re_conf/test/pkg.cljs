(ns re-conf.test.pkg
  (:require
   [cljs.core.async :refer [<! go]]
   [re-conf.resources.pkg :refer (package installed? initialize)]
   [cljs.test :refer-macros  [deftest is testing async]]))

(defn ok?  [m]
  (contains? m :ok))

(deftest packaging
  (async done
         (go
           (let [_ (initialize)
                 present (<! (package "libxml2-dev"))
                 installed (<! (installed? "libxml2-dev"))
                 absent (<! (package "libxml2-dev" :absent))]
             (is (ok? present))
             (is (ok? installed))
             (is (ok? absent))
             (done)))))
