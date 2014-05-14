(defproject com.palletops/docudata "0.1.0-SNAPSHOT"
  :description "Library for extracting documentation from code."
  :url "https://palletops.com/docudata"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[bultitude "0.2.6"]
                 [fipp "0.4.2"]]
  :plugins [[lein-modules "0.3.1"]]
  :profiles {:provided {:dependencies [[org.clojure/clojure "1.6.0"]]}})
