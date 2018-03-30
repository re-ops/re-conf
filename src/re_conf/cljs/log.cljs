(ns re-conf.cljs.log
  "node logging")

(def winston (js/require "winston"))

(def settings
  (let [f (clj->js {"filename" "re-conf.log"})]
    {:level "info"
     :transports [(winston.transports.Console.)
                  (winston.transports.File. f)]}))

(def logger (.createLogger winston (clj->js settings)))

(defn info
  "Using winston logging info"
  [m]
  (.info logger m))

(comment
  (info "hello"))

