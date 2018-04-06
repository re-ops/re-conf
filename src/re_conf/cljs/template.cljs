(ns re-conf.cljs.template
  "Template resource"
  (:require
   [re-conf.cljs.log :refer (info)]
   [cljs.core.async :as async :refer [<! go]]
   [cljs-node-io.core :as io]
   [cljstache.core :refer [render]]))

(defn template
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

(comment
  (info
   (template {:keys {:key "abcd" :user "foo@bla"}} "resources/authorized_keys.mustache" "/tmp/keys") ::template))
