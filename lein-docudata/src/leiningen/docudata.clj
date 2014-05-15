(ns leiningen.docudata
  (:require
   [clojure.java.io :refer [file]]
   [leiningen.core.eval :refer [eval-in-project]]
   [leiningen.core.project
    :refer [add-profiles merge-profiles unmerge-profiles]]))

(def profiles
  {::docudata
   {:dependencies [['com.palletops/docudata "0.1.0-SNAPSHOT"]]}})

(defn docudata-options
  [project]
  (merge
   {:output-file (str (file (:target-path project) "docudata.edn"))
    :source-paths (:source-paths project)}
   (:docudata project)))

(defn docudata
  "Generate edn file with documentation for clojure code on source paths."
  [project & args]
  (let [project (if (get-in (meta project) [:profiles :docudata])
                  (merge-profiles project [:docudata])
                  project)
        project (-> project
                    (unmerge-profiles [:default])
                    (add-profiles profiles)
                    (merge-profiles [::docudata]))]
    (eval-in-project
     project
     `(com.palletops.docudata/generate
       '~(select-keys project
                      [:group :name :version :description :dependencies])
       '~(docudata-options project))
     `(require 'com.palletops.docudata))))
