(ns re-conf.rcp.basic
  "Basic setup"
  (:require
   [re-conf.cljs.download :refer (download checksum)]
   [re-conf.cljs.archive :refer (unzip)]
   [re-conf.cljs.shell :refer (exec)]
   [re-conf.cljs.output :refer (summary)]))

