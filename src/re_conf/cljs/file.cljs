(ns re-conf.cljs.file
  "File resources"
  (:require
   [re-conf.cljs.common :refer (run)]
   [re-conf.cljs.log :refer (info)]
   [cljs.core.async :as async :refer [<! go]]
   [cljs-node-io.core :as io]
   [cljstache.core :refer [render]]))

(def fs (js/require "fs"))

(defn- run-template
  "Create a file from a template with args"
  [args tmpl dest]
  (go
    (let [[esl s] (<! (io/aslurp tmpl))]
      (if esl
        {:error {:message "reading template source failed" :error esl :source tmpl}}
        (let [[esp] (<! (io/aspit dest (render s args)))]
          (if esp
            {:error {:message "reading template source failed" :error esp :source tmpl}}
            {:ok {:message "writing template source success" :template tmpl :dest dest}}))))))

(defn run-chown
  "Change folder owner and group"
  [dest usr grp])

(defn template
  "File template resource"
  ([args tmpl dest]
   (template nil args tmpl dest))
  ([c args tmpl dest]
   (run c (fn [] (run-template args tmpl dest)))))

(comment)
