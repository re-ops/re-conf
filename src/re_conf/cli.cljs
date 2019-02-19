(ns re-conf.cli
  (:require-macros
   [clojure.core.strint :refer (<<)])
  (:require
   [re-conf.resources.log :as log :refer (debug-on)]
   [clojure.tools.cli :refer [parse-opts]]))

(def process (js/require "process"))

(def fs (js/require "fs"))

(defn into-categories [s]
  (map keyword (clojure.string/split s #"\,")))

(defn- cli-options
  [categories]
  [["-d" "--debug" "debug (include profiling information)" :default false]
   ["-e" "--environment ENVIRONMENT" "environment file"
    :validate [#(.existsSync fs %) "environment file is missing make sure that path is correct"]]
   ["-c" "--categories CATEGORIES" (<< "comma seperated list of categories from the available ~(map name (keys categories)) list")
    :validate [#(every? (set (keys categories)) (into-categories %)) (<< "each category must be one of ~(keys categories)")]]
   ["-h" "--help"]])

(defn- pre-process
  "catch help and errors"
  [{:keys [summary errors options] :as m}]
  (when-let [help (options :help)]
    (println summary)
    (.exit process 0))
  (when-not (empty? errors)
    (doseq [e errors]
      (println e))
    (.exit process 1))
  (when-not (contains? options :environment)
    (println "environment is missing")
    (.exit process 1))
  (when-not (contains? options :categories)
    (println "categories are missing")
    (.exit process 1))
  (when (options :debug)
    (debug-on))
  m)

(defn parse-options
  [args categories]
  (pre-process (parse-opts args (cli-options categories))))

