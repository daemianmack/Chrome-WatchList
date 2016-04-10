(defproject binaryage/chromex-sample "0.1.0-SNAPSHOT"
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
                          {:content-script
                           {:source-paths ["src/content_script"]
                            :compiler     {:output-to             "resources/unpacked/compiled/content_script/chromex-sample.js"
                                           :output-dir            "resources/unpacked/compiled/content_script"
                                           :asset-path            "compiled/content_script"
                                           :optimizations         :whitespace                                                 ; content scripts cannot do eval / load script dynamically
                                           :main                  "chromex-sample.content-script.core"
                                           :anon-fn-naming-policy :unmapped
                                           :pretty-print          true
                                           :compiler-stats        true
                                           :cache-analysis        true
                                           :source-map            "resources/unpacked/compiled/content_script/chromex-sample.js.map"
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

  :aliases {"content"   ["with-profile" "+unpacked" "cljsbuild" "auto" "content-script"]
            "release"   ["with-profile" "+release" "do" "clean," "cljsbuild" "once" "content-script"]
            "package"   ["shell" "scripts/package.sh"]})
