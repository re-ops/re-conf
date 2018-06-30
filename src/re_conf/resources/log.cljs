(ns re-conf.resources.log
  "Logging facade"
  (:require-macros
   [clojure.core.strint :refer (<<)])
  (:require
   [clojure.string :refer (replace-first)]
   [cljs.core.async :as async :refer [take!]]))

(def winston (js/require "winston"))

(def os (js/require "os"))

(def hostname (.hostname os))

(defn channel?
  "check is x is a channel"
  [x]
  (= (type x) cljs.core.async.impl.channels/ManyToManyChannel))

(defn settings [level]
  (let [f (clj->js {"filename" "re-conf.log" "colorize" true})]
    (clj->js
     {:level level
      :format (winston.format.combine (winston.format.timestamp) (winston.format.json))
      :transports [(winston.transports.File. f)]})))

(defn console-format []
  (winston.format.printf
   (fn [info]
     (let [date (.toLocaleString (js/Date.))
           level (.-level info)
           n (replace-first (name (.-ns info)) ":" "")
           message (.-message info)
           out (if (string? message) message (.stringify js/JSON message nil 1))]
       (<< "~{date} ~{hostname} ~{level} [~{n}] - ~{out}")))))

(defn setup-logger [level]
  (let [base (.createLogger winston (settings level))
        fmt (winston.format.combine (winston.format.colorize) (console-format))]
    (.add base (winston.transports.Console. (clj->js {:format fmt})))))

(def logger (atom (setup-logger "info")))

(defn debug-on []
  (reset! logger (setup-logger "debug")))

(defn debug-off []
  (reset! logger (setup-logger "info")))

(defn- winston-log
  "Using Winston to log"
  [m n level]
  (.log @logger (clj->js {:level level :message m :ns (str n)})))

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
  (info {:fo 1 :bla 2 :biiiggg ""} ::log)
  (error "bla" ::log)
  (debug-on))

