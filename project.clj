(defproject featured-filter "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-time "0.13.0"]
                 [cheshire "5.7.1"]
                 [com.cognitect/transit-clj "0.8.300"]
                 [com.fasterxml.jackson.core/jackson-core "2.8.9"]]
  :main ^:skip-aot featured-filter.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
