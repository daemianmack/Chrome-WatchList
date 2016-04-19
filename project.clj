(defproject watchlist "0.2.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.7.228"]
                 [reagent  "0.5.1"]
                 [re-frame "0.7.0"]]

  :plugins [[lein-cljsbuild "1.1.1"]]

  :source-paths ["src"]

  :clean-targets ^{:protect false} ["target"
                                    "resources/unpacked/compiled"
                                    "resources/release/compiled"]

  :cljsbuild {:builds {}}                                                                                                     ; prevent https://github.com/emezeske/lein-cljsbuild/issues/413

  :profiles {:unpacked
             {:cljsbuild {:builds
                          {:watchlist
                           {:source-paths ["src/watchlist" "src/extension"]
                            :compiler     {:output-to             "resources/unpacked/compiled/watchlist.js"
                                           :output-dir            "resources/unpacked/compiled/watchlist"
                                           :asset-path            "compiled/watchlist"
                                           :optimizations         :whitespace
                                           :main                  "watchlist.core"
                                           :anon-fn-naming-policy :unmapped
                                           :pretty-print          true
                                           :compiler-stats        true
                                           :cache-analysis        true
                                           :source-map            "resources/unpacked/compiled/watchlist.js.map"
                                           :source-map-timestamp  true}}}}}
             :release
             {:env       {:chromex-elide-verbose-logging "true"}
              :cljsbuild {:builds
                          {:content-script
                           {:source-paths ["src/content_script"]
                            :compiler     {:output-to      "resources/release/compiled/content_script.js"
                                           :output-dir     "resources/release/compiled/content_script"
                                           :asset-path     "compiled/content_script"
                                           :optimizations  :advanced
                                           :elide-asserts  true
                                           :compiler-stats true}}}}}}

  :aliases {"dev"     ["with-profile" "+unpacked" "cljsbuild" "auto" "watchlist"]
            "release" ["with-profile" "+release" "do" "clean," "cljsbuild" "once" "watchlist"]
            "package" ["shell" "scripts/package.sh"]})
