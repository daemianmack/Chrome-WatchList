(defproject watchlist "0.2.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.7.228"]
                 [reagent  "0.5.1"]
                 [re-frame "0.7.0"]]

  :plugins [[lein-cljsbuild "1.1.1"]]

  :source-paths []

  :clean-targets ^{:protect false} ["target"
                                    "resources/unpacked/compiled"
                                    "resources/test/compiled"]

  :cljsbuild {:builds
              {:watchlist {:source-paths ["src/watchlist" "src/extension"]
                           :compiler     {:output-to             "resources/unpacked/compiled/watchlist.js"
                                          :output-dir            "resources/unpacked/compiled/watchlist"
                                          :asset-path            "compiled/watchlist"
                                          :optimizations         :whitespace
                                          :main                  "extension.chrome"
                                          :anon-fn-naming-policy :unmapped
                                          :pretty-print          true
                                          :compiler-stats        true
                                          :cache-analysis        true
                                          :source-map            "resources/unpacked/compiled/watchlist.js.map"
                                          :source-map-timestamp  true}}
               :test {:source-paths ["src/watchlist" "test"]
                      :notify-command ["phantomjs" "phantom/runner.js" "resources/index.html"]
                      :compiler     {:output-to             "resources/test/compiled/watchlist.js"
                                     :output-dir            "resources/test/compiled/watchlist"
                                     :optimizations         :whitespace}}}}

  :aliases {"dev"     ["cljsbuild" "auto" "watchlist"]
            "release" ["do" "clean," "cljsbuild" "once" "watchlist"]
            "test"    ["do" "clean," "cljsbuild" "auto" "test"]
            "package" ["shell" "scripts/package.sh"]})
