(ns re-conf.cli
  (:require-macros
   [clojure.core.strint :refer (<<)])
  (:require
   [re-conf.resources.log :as log :refer (debug-on)]
   [clojure.tools.cli :refer [parse-opts]]))

(def process (js/require "process"))

(def fs (js/require "fs"))

(defn- cli-options
  [profiles]
  [["-d" "--debug" "debug (include profiling information)" :default false]
   ["-e" "--environment ENVIRONMENT" "environment file"
    :default "resources/dev.edn"
    :validate [#(.existsSync fs %) "environment file missing"]]
   ["-p" "--profile PROFILE" (<< "profile, one of ~{profiles}")
    :default (first profiles)
    :validate [#(profiles (keyword %)) (<< "profile must be one of ~{profiles}")]]
   ["-h" "--help"]])

(defn- pre-process
  "catch help and errors"
  [{:keys [summary errors options] :as m}]
  (when-let [help (options :help)]
    (println help)
    (.exit process 0))
  (when-not (empty? errors)
    (doseq [e errors]
      (println e))
    (.exit process 1))
  (when (options :debug)
    (debug-on))
  m)

(defn parse-options
  [args profiles]
  (pre-process (parse-opts args (cli-options profiles))))

