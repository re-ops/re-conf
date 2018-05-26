(defproject re-conf "0.1.6"
  :description "Machine configuration and spec recipes"
  :url "https://github.com/re-ops/re-conf"
  :license  {:name "Apache License, Version 2.0" :url "http://www.apache.org/licenses/LICENSE-2.0.html"}

  :min-lein-version "2.7.1"

  :dependencies [[org.clojure/clojure "1.9.0"]
                 ; common
                 [org.clojure/core.async "0.4.474"]

                 ; clojurescript
                 [org.clojure/clojurescript "1.10.238"]
                 [cljs-node-io "0.5.0"]

                 ; string manipulation
                 [funcool/cuerdas "2.0.5"]

                 ; << macro
                 [org.clojure/core.incubator "0.1.4"]

                 ; matching
                 [org.clojure/core.match "0.3.0-alpha5"]

                 ; templates
                 [cljstache "2.0.1"]
                 ]
  :npm {
        :dependencies [
          ["request" "2.85.0"]
        ]

        :devDependencies[
          ["source-map-support" "^0.4.15"]
          ["ws" "^0.8.1"]
          ["winston" "3.0.0-rc3"]
          ["systeminformation" "3.37.9"]
        ]
  }

  :plugins [[lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]
            [lein-figwheel "0.5.14"]
            [lein-cljfmt "0.5.7"]
            [lein-set-version "0.3.0"]
            [lein-tag "0.1.0"]
            [lein-npm "0.6.2"]]


  :source-paths ["src"]

  :clean-targets ["server.js" "target"]

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src" "re-base/src"]
              :figwheel true
              :compiler {
                :main re-conf.core
                :asset-path "target/js/compiled/dev"
                :output-to "target/js/compiled/re-conf.js"
                :output-dir "target/js/compiled/dev"
                :target :nodejs
                :optimizations :none
                :source-map-timestamp true
		   }
             }
             {:id "test"
              :source-paths ["src" "test"]
              :notify-command ["node" "target/unit-tests.js"]
              :compiler {
                 :output-to "target/unit-tests.js"
                 :optimizations :none
                 :target :nodejs
               :main re-conf.test.suite
		   }
		 }
             {:id "prod"
              :source-paths ["src"]
              :compiler {
                :output-to "re-conf.js"
                :output-dir "target/js/compiled/prod"
                :target :nodejs
                :optimizations :simple}}]}

    :profiles {
      :dev {
         :dependencies
            [[figwheel-sidecar "0.5.14"] [com.cemerick/piggieback "0.2.2"]]
          :source-paths ["src" "dev"]
          :repl-options {
            :port 38081
            :init (do (fig-start))
            :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]
           }
       }
      :codox {
       :dependencies [[org.clojure/tools.reader "1.1.0"]
                      [codox-theme-rdash "0.1.2"]]
       :plugins [[lein-codox "0.10.3"]]
       :codox {:project {:name "re-mote"}
               :themes [:rdash]
               :language :clojurescript
               :source-paths ["src"]
               :source-uri "https://github.com/re-ops/re-conf/blob/master/{filepath}#L{line}"
       }
     }
    }

    :aliases {
       "travis" [
         "do" "clean," "cljfmt" "check," "npm" "install," "cljsbuild" "once" "prod," "cljsbuild" "test"
       ]
      "docs" [
        "with-profile" "codox" "do" "codox"
      ]
    }
)
