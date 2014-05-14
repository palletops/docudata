;; This project is a parent project only, and does not generate any artifacts
(defproject com.palletops.not-published/docudata "0.1.0-SNAPSHOT"
  :description "Generate data for documentation."
  :url "https://github.com/palletops/docudata"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-modules "0.3.1"]]
  :aliases {"install" ["modules" "install"]
            "deploy" ["modules" "deploy"]
            "clean" ["modules" "clean"]})
