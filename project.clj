(defproject watchlist "0.2.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.7.228"]
                 [reagent  "0.5.1"]
                 [re-frame "0.7.0"]]

  :plugins [[lein-cljsbuild "1.1.1"]
            [lein-shell "0.4.2"]]

  :source-paths []

  :clean-targets ["builds"]

  :cljsbuild {:builds
              {:dev {:source-paths ["src/watchlist" "src/extension"]
                     :compiler     {:output-to             "builds/dev/compiled/watchlist.js"
                                    :output-dir            "builds/dev/compiled/watchlist"
                                    :asset-path            "compiled/watchlist"
                                    :optimizations         :whitespace
                                    :main                  "extension.chrome"
                                    :anon-fn-naming-policy :unmapped
                                    :pretty-print          true
                                    :cache-analysis        true
                                    :source-map            "builds/dev/compiled/watchlist.js.map"
                                    :source-map-timestamp  true}}

               :test {:source-paths ["src/watchlist" "test"]
                      :compiler     {:output-to     "builds/test/compiled/watchlist.js"
                                     :output-dir    "builds/test/compiled/watchlist"
                                     :optimizations :whitespace}
                      :notify-command ["phantomjs" "phantom/runner.js" "resources/index.html"]}

               :prod {:source-paths ["src/watchlist" "src/extension"]
                      :compiler     {:output-to     "builds/prod/compiled/watchlist.js"
                                     :output-dir    "builds/prod/compiled/watchlist"
                                     :asset-path    "compiled/watchlist"
                                     :optimizations :advanced
                                     :externs       ["resources/externs.js"]}}}}

  :aliases {"dev"      ["do" "clean," "shell" "scripts/insert_assets.sh" "builds/dev,"  "cljsbuild" "auto" "dev"]
            "prod"     ["do" "clean," "shell" "scripts/insert_assets.sh" "builds/prod," "cljsbuild" "once" "prod"]
            "autotest" ["do" "clean," "cljsbuild" "auto" "test" "dev"]
            "package"  ["shell" "scripts/package.sh"]})
