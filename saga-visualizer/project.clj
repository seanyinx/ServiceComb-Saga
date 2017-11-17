(defproject saga-visualizer "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.671"]
                 [reagent "0.7.0"]]
  :jvm-opts ^:replace ["-Xmx1g" "-server"]
  :plugins [[lein-npm "0.6.2"]
            [lein-cljsbuild "1.1.4"
             :exclusions [org.clojure/clojure]]
            [lein-figwheel "0.5.14"]]
  :npm {:dependencies [[source-map-support "0.4.0"]]}
  :source-paths ["src" "target/classes"]
  :clean-targets ^{:protect false} ["resources/public/js"]
  :target-path "target"

  :cljsbuild {
              :builds [{:id           "saga-visualizer"
                        :source-paths ["src"]
                        :figwheel     true
                        :compiler     {
                                       :main                 saga-visualizer.core
                                       :asset-path           "js/out"
                                       :output-to            "resources/public/js/saga_visualizer.js"
                                       :output-dir           "resources/public/js/out"
                                       :source-map-timestamp true}}]})
