(defproject shime "0.0.1"
    :min-lein-version "2.7.1"

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.9.229"]]

  :plugins [[lein-figwheel "0.5.9"]]

  :source-paths ["src"]

  :cljsbuild {:builds
              [{:id "node-dev"
                :source-paths ["src"]
                :figwheel true
                :compiler {:main shim.core
                           :asset-path "target/js/compiled/out"
                           :output-to  "target/js/compiled/node_example.js"
                           :output-dir "target/js/compiled/out"
                           :source-map-timestamp true
                           ;; !!! need to set the target to :nodejs !!!
                           :target :nodejs}}]}

  :profiles {:dev {:dependencies [[figwheel-sidecar "0.5.9"]
                                  [com.cemerick/piggieback "0.2.1"]]
                   :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}}
)
