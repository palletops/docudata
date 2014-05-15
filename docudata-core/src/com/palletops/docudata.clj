(ns com.palletops.docudata
  "Extract documentation data to edn."
  (:require
   [com.palletops.docudata.extract :refer [docudata]]
   [fipp.edn :refer [pprint]]))

(defn generate
  "Generate documentation edn file for project."
  [{:keys [group name version description] :as project}
   {:keys [output-file source-paths exclude-keys] :as options}]
  (println "Generate docudata in" output-file "for" (pr-str source-paths))
  (spit output-file
        (with-out-str
          (pprint
           (assoc project
             :namespaces (docudata source-paths options))))))
