(ns re-conf.main
  (:require
   [re-conf.resources.common :refer (profile function-name)]))

(defn -main [& args]
  (println (str identity))
  (println (function-name identity)))

(set! *main-cli-fn* -main)
