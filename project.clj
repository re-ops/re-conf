(defproject shim "0.0.1"
  :description "Portable configuration management language"
  :url "https://github.com/narkisr/shim"
  :license  {:name "Apache License, Version 2.0" :url "http://www.apache.org/licenses/LICENSE-2.0.html"}

  :min-lein-version "2.7.1"

  :dependencies [[org.clojure/clojure "1.9.0"]

                 ; common
                 [org.clojure/core.async "0.4.474"]
                 [fipp "0.6.12"]
                 [eval-soup "1.4.1"]

                 ; clojurescript
                 [org.clojure/clojurescript "1.10.126"]
                 [cljs-node-io "0.5.0"]
                 [com.taoensso/timbre "4.10.0"]

                 ; string manipulation
                 [funcool/cuerdas "2.0.5"]

                 ]

  :plugins [[lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]
            [lein-figwheel "0.5.14"]]

  :source-paths ["src"]

  :clean-targets ["server.js" "target"]

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src"]
              :figwheel true
              :compiler {
                :main shim.cljs.core
                :asset-path "target/js/compiled/dev"
                :output-to "target/js/compiled/shim.js"
                :output-dir "target/js/compiled/dev"
                :target :nodejs
                :optimizations :none
                :source-map-timestamp true}}
             {:id "prod"
              :source-paths ["src"]
              :compiler {
                :output-to "server.js"
                :output-dir "target/js/compiled/prod"
                :target :nodejs
                :optimizations :simple}}]}

  :profiles {
      :dev {
         :dependencies
            [[figwheel-sidecar "0.5.14"] [com.cemerick/piggieback "0.2.2"]]
          :source-paths ["src" "dev"]
          :repl-options {
            :init (do (fig-start))
            :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]
           }
       }})
