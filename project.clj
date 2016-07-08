(defproject watchlist "0.5.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.7.228"]
                 [reagent  "0.5.1"]
                 [re-frame "0.7.0"]]

  :plugins [[lein-cljsbuild "1.1.1"]
            [lein-chromebuild "0.3.0"]]

  :source-paths []

  :cljsbuild {:builds
              {:dev {:source-paths ["src/watchlist" "src/extension"]
                     :compiler     {:output-to             "target/unpacked/watchlist.js"
                                    :output-dir            "target/unpacked/watchlist"
                                    :asset-path            "compiled/watchlist"
                                    :optimizations         :whitespace
                                    :main                  "extension.chrome"
                                    :anon-fn-naming-policy :unmapped
                                    :pretty-print          true
                                    :cache-analysis        true
                                    :source-map            "target/unpacked/watchlist.js.map"
                                    :source-map-timestamp  true}}

               :test {:source-paths ["src/watchlist" "test"]
                      :compiler     {:output-to     "builds/test/compiled/watchlist.js"
                                     :output-dir    "builds/test/compiled/watchlist"
                                     :optimizations :whitespace}
                      :notify-command ["phantomjs" "phantom/runner.js" "resources/testing/index.html"]}

               :prod {:source-paths ["src/watchlist" "src/extension"]
                      :compiler     {:output-to     "builds/prod/compiled/watchlist.js"
                                     :output-dir    "builds/prod/compiled/watchlist"
                                     :asset-path    "compiled/watchlist"
                                     :optimizations :advanced
                                     :externs       ["resources/externs.js"]}}

  :aliases {"dev"      ["do" "clean," "shell" "scripts/insert_assets.sh" "builds/dev,"  "cljsbuild" "auto" "dev"]
               :options {:source-paths ["src/options"]
                         :compiler     {:output-to             "target/unpacked/options.js"
                                        :output-dir            "target/unpacked/options"
                                        :asset-path            "compiled/options"
                                        :optimizations         :whitespace
                                        :main                  "options.ui/main"
                                        :anon-fn-naming-policy :unmapped
                                        :pretty-print          true
                                        :cache-analysis        true
                                        :source-map            "target/unpacked/options.js.map"
                                        :source-map-timestamp  true}}}}

  :chromebuild {:resource-paths ["resources/assets/js"
                                 "resources/assets/images"
                                 "resources/assets/css"]
                :target-path "target/unpacked"}
            "prod"     ["do" "clean," "shell" "scripts/insert_assets.sh" "builds/prod," "cljsbuild" "once" "prod"]
            "autotest" ["do" "clean," "cljsbuild" "auto" "test"]
            "package"  ["shell" "scripts/package.sh"]})
