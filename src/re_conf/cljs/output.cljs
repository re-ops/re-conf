(ns re-conf.cljs.output
  (:require
    [cljs.core.match :refer-macros  [match]]
    [re-conf.cljs.log :refer (info debug error)]
    [cljs.core.async :as async :refer [take!]]
    )
 )

(defn summary
  "Print result"
  ([c]
   (summary c "Pipeline ok"))
  ([c m]
   (take! c
          (fn [r]
            (match r
              {:error e} (error e ::summary-fail)
              {:ok o} (info m ::summary-ok)
              :else (error r ::summary-error))))))
