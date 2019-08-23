(defproject ct.chat "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.10.0"]
                 [ring-server "0.5.0"]
                 [reagent "0.8.1"]
                 [reagent-utils "0.3.2"]
                 [ring "1.7.1"]
                 [ring/ring-defaults "0.3.2"]
                 [hiccup "1.0.5"]
                 [yogthos/config "1.1.1"]
                 [org.clojure/clojurescript "1.10.439"
                  :scope "provided"]
                 [metosin/reitit "0.2.10"]
                 [venantius/accountant "0.2.4"
                  :exclusions [org.clojure/tools.reader]]
                 [re-frame "0.10.6"]
                 [org.clojure/data.xml "0.2.0-alpha6"]

                 [rhizome "0.2.7"]
                 [primitive-math "0.1.5"]
                 [potemkin "0.4.3"]
                 [proteus "0.1.6"]

                 [buddy/buddy-sign "3.1.0"]
                 [clj-time "0.15.2"]]

  :plugins [[lein-environ "1.1.0"]
            [lein-cljsbuild "1.1.7"]
            [lein-asset-minifier "0.2.7"
             :exclusions [org.clojure/clojure]]]

  :ring {:handler ct.chat.handler/app
         :uberwar-name "ct.chat.war"}

  :min-lein-version "2.5.0"
  :uberjar-name "ct.chat.jar"
  :main ct.chat.server
  :clean-targets ^{:protect false}
  [:target-path
   [:cljsbuild :builds :app :compiler :output-dir]
   [:cljsbuild :builds :app :compiler :output-to]]

  :source-paths ["src/clj" "src/cljc"]
  :resource-paths ["resources" "target/cljsbuild"]

  :minify-assets
  {:assets
   {"resources/public/css/site.min.css" "resources/public/css/site.css"}}

  :cljsbuild
  {:builds {:min
            {:source-paths ["src/cljs" "src/cljc" "env/prod/cljs"]
             :compiler
             {:output-to        "target/cljsbuild/public/js/app.js"
              :output-dir       "target/cljsbuild/public/js"
              :source-map       "target/cljsbuild/public/js/app.js.map"
              :optimizations :advanced
              :pretty-print  false
              :closure-defines {"goog.DEBUG" false}}}

            :app
            {:source-paths ["src/cljs" "src/cljc" "env/dev/cljs"]
             :figwheel {:on-jsload "ct.chat.core/mount-root"}
             :compiler
             {:main "ct.chat.dev"
              :asset-path "/js/out"
              :output-to "target/cljsbuild/public/js/app.js"
              :output-dir "target/cljsbuild/public/js/out"
              :source-map true
              :optimizations :none
              :pretty-print  true
              :closure-defines {"re_frame.trace.trace_enabled_QMARK_" true}
              :preloads [day8.re-frame-10x.preload]
              :infer-externs true
              :npm-deps false
              :foreign-libs [{:file "target/foreign_libs/mediasoup_client.js"
                              :provides ["mediasoup-client"]
                              :global-exports {mediasoup-client mediasoupClient}}
                             {:file "target/foreign_libs/protoo_client.js"
                              :provides ["protoo-client"]
                              :global-exports {protoo-client protooClient}}]}}

            :test
            {:source-paths ["src/cljs" "src/cljc" "test/cljs"]
             :compiler {:main ct.chat.doo-runner
                        :asset-path "/js/out"
                        :output-to "target/test.js"
                        :output-dir "target/cljstest/public/js/out"
                        :optimizations :whitespace
                        :pretty-print true}}

            :devcards
            {:source-paths ["src/cljs" "src/cljc" "env/dev/cljs"]
             :figwheel {:devcards true}
             :compiler {:main "ct.chat.cards"
                        :asset-path "js/devcards_out"
                        :output-to "target/cljsbuild/public/js/app_devcards.js"
                        :output-dir "target/cljsbuild/public/js/devcards_out"
                        :source-map-timestamp true
                        :optimizations :none
                        :pretty-print true}}
            }
   }
   :doo {:build "test"
         :alias {:default [:chrome]}}

  :figwheel
  {:http-server-root "public"
   :server-port 3449
   :nrepl-port 7002
   :nrepl-middleware [cider.piggieback/wrap-cljs-repl
                      cider.nrepl/cider-middleware
                      #_refactor-nrepl.middleware/wrap-refactor]
   :css-dirs ["resources/public/css"]
   :ring-handler ct.chat.handler/app}


  :sass {:source-paths ["src/sass"]
         :target-path "resources/public/css"}

  :profiles {:dev {:repl-options {:init-ns ct.chat.repl}
                   :dependencies [#_[cider/piggieback "0.3.10"]
                                  [binaryage/devtools "0.9.10"]
                                  [ring/ring-mock "0.3.2"]
                                  [ring/ring-devel "1.7.1"]
                                  [prone "1.6.1"]
                                  [figwheel-sidecar "0.5.18"]
                                  #_[nrepl "0.5.3"]
                                  [devcards "0.2.6" :exclusions [cljsjs/react]]
                                  [pjstadig/humane-test-output "0.9.0"]

                                  ;; To silence warnings from sass4clj dependecies about missing logger implementation
                                  [org.slf4j/slf4j-nop "1.7.25"]

                                  [day8.re-frame/re-frame-10x "0.4.0"]]

                   :source-paths ["env/dev/clj"]
                   :plugins [[lein-figwheel "0.5.18"]
                             [lein-doo "0.1.10"]
                             #_[cider/cider-nrepl "0.19.0"]
                             [org.clojure/tools.namespace "0.3.0-alpha4"
                              :exclusions [org.clojure/tools.reader]]
                             #_[refactor-nrepl "2.4.0"
                              :exclusions [org.clojure/clojure]]
                             [deraen/lein-sass4clj "0.3.1"]
                             ]

                   :injections [(require 'pjstadig.humane-test-output)
                                (pjstadig.humane-test-output/activate!)]

                   :env {:dev true}}

             :uberjar {:hooks [minify-assets.plugin/hooks]
                       :source-paths ["env/prod/clj"]
                       :prep-tasks ["compile" ["cljsbuild" "once" "min"]]
                       :env {:production true}
                       :aot :all
                       :omit-source true}})
