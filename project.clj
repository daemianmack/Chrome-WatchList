(defproject watchlist "0.6.0"
  :dependencies [[org.clojure/clojure "1.9.0-alpha15"]
                 [org.clojure/clojurescript "1.9.494"]
                 [com.lucasbradstreet/instaparse-cljs "1.4.1.2"]
                 [hipo "0.5.2"]
                 [cljsjs/xregexp "3.1.1-0"]]

  :plugins [[lein-cljsbuild "1.1.5"]
            [lein-shell "0.4.2"]
            [lein-chromebuild "0.3.0"]]

  :source-paths ["src/watchlist" "src/extension" "src/options"]

  :cljsbuild {:builds
              {:dev {:source-paths ["src/common" "src/watchlist" "src/extension"]
                     :compiler     {:output-to             "target/unpacked/watchlist.js"
                                    :output-dir            "target/unpacked/watchlist"
                                    :optimizations         :whitespace
                                    :main                  "extension.chrome"
                                    ;; Work around issue raised with CLJS compiler commit 404d6444cb641
                                    ;; More here: http://dev.clojure.org/jira/browse/CLJS-1547?focusedCommentId=42617
                                    :closure-output-charset "US-ASCII"
                                    :anon-fn-naming-policy :unmapped
                                    :pretty-print          true
                                    :cache-analysis        true
                                    :source-map            "target/unpacked/watchlist.js.map"
                                    :source-map-timestamp  true}}

               :options {:source-paths ["src/common" "src/options"]
                         :compiler     {:output-to             "target/unpacked/options.js"
                                        :output-dir            "target/unpacked/options"
                                        :optimizations         :whitespace
                                        ;; Work around issue raised with CLJS compiler commit 404d6444cb641
                                        ;; More here: http://dev.clojure.org/jira/browse/CLJS-1547?focusedCommentId=42617
                                        :closure-output-charset "US-ASCII"
                                        :anon-fn-naming-policy :unmapped
                                        :pretty-print          true
                                        :cache-analysis        true
                                        :source-map            "target/unpacked/options.js.map"
                                        :source-map-timestamp  true}}

               :options-prod {:source-paths ["src/common" "src/options"]
                              :compiler     {:output-to     "target/options-prod/options.js"
                                             :output-dir    "target/options-prod/options"
                                             :optimizations :advanced
                                             :externs       ["resources/externs.js"]}}

               :test {:source-paths ["src/common" "src/watchlist" "test"]
                      :compiler     {:output-to     "target/test/watchlist.js"
                                     :output-dir    "target/test/watchlist"
                                     :optimizations :whitespace
                                     :externs       ["resources/externs.js"]}
                      :notify-command ["phantomjs" "phantom/runner.js" "resources/testing/index.html"]}

               :prod {:source-paths ["src/common" "src/watchlist" "src/extension"]
                      :compiler     {:output-to     "target/prod/watchlist.js"
                                     :output-dir    "target/prod/watchlist"
                                     :optimizations :advanced
                                     :externs       ["resources/externs.js"]}}}}

  :chromebuild {:resource-paths ["resources/assets"]
                :target-path "target/unpacked"}

  :resource-paths ["resources"]

  :aliases {"dev"      ["do" "clean,"
                        "shell" "scripts/insert_assets.sh" "target/unpacked,"
                        "cljsbuild" "auto" "dev" "options"]
            "prod"     ["do" "clean,"
                        "cljsbuild" "once" "prod" "options-prod,"
                        "shell" "scripts/package.sh"]
            "autotest" ["do" "clean," "cljsbuild" "auto" "test"]})
