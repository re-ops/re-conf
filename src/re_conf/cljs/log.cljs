(ns re-conf.cljs.log
  "Logging facade"
  (:require
   [cljs.core.async :as async :refer [take!]]
   [re-conf.cljs.common :refer (channel?)]))

(def winston (js/require "winston"))

(def settings
  (let [f (clj->js {"filename" "re-conf.log"})]
    {:level "debug"
     :format (winston.format.combine (winston.format.timestamp) (winston.format.json))
     :transports [(winston.transports.Console.)
                  (winston.transports.File. f)]}))

(def logger (.createLogger winston (clj->js settings)))

(defn- winston-log
  "Using Winston to log"
  [m n level]
  (.log logger (clj->js {:level level :message m :ns (str n)})))

(defn- log
  [m n level]
  (if (channel? m)
    (take! m (fn [o] (winston-log o n level)))
    (winston-log m n level)))

(defn error
  [m n]
  (log m n "error"))

(defn info
  [m n]
  (log m n "info"))

(defn debug
  [m n]
  (log m n "debug"))

(comment
  (info "hello" ::log)
  (debug "bla" ::log))

