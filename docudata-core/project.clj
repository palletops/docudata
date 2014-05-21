(defproject com.palletops/docudata "0.1.1-SNAPSHOT"
  :description "Library for extracting documentation from code."
  :url "https://palletops.com/docudata"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[bultitude "0.2.6"]
                 [fipp "0.4.2"]
                 [scout "0.1.1"]
                 [me.raynes/fs "1.4.4"]
                 [pathetic "0.5.1"]]
  :plugins [[lein-modules "0.3.1"]]
  :profiles {:provided {:dependencies [[org.clojure/clojure "1.6.0"]]}})
