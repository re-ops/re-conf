(ns re-conf.cljs.basic
  (:require [re-conf.cljs.core :refer (install checksum -!>)]))

(defn sanity []
  (-!> (checksum "/tmp/form-init2576307926037317786.clj" :md5)))

(comment
  (sanity))
