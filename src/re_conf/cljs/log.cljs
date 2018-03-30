(ns re-conf.cljs.log
  "node logging")

(def winston (js/require "winston"))

(def settings
  (let [f (clj->js {"filename" "re-conf.log"})]
    {:level "debug"
     :format (winston.format.combine (winston.format.timestamp) (winston.format.json))
     :transports [(winston.transports.Console.)
                  (winston.transports.File. f)]}))

(def logger (.createLogger winston (clj->js settings)))

(defn- log
  [m n level]
  (.log logger (clj->js {:level level :message m :ns (str n)})))

(defn error
  "Using Winston logging info"
  [m n]
  (log m n "error"))

(defn info
  "Using Winston logging info"
  [m n]
  (log m n "info"))

(defn debug
  "Using Winston logging info"
  [m n]
  (log m n "debug"))

(comment
  (info "hello" ::log)
  (debug "bla" ::log))

