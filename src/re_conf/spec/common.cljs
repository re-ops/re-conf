(ns re-conf.spec.common
  (:require
   [cljs.spec.alpha :as s]
   [re-conf.resources.log :refer (info debug error)]
   [expound.alpha :as expound]))

(defn valid? [spec value]
  (if (s/valid? spec value)
    true
    (do
      (error (expound/expound spec value) ::file)
      false)))
