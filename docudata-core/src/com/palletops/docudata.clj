(ns com.palletops.docudata
  "Extract documentation data to edn."
  (:require
   [com.palletops.docudata.extract :refer [docudata snippets-in-path]]
   [fipp.edn :refer [pprint]]))

(defn generate
  "Generate documentation edn file for project."
  [{:keys [group name version description] :as project}
   {:keys [output-file source-paths exclude-keys root] :as options}]
  (println "Generate docudata in" output-file "for" (pr-str source-paths))
  (let [namespaces (doall (docudata source-paths options))
        snippets (doall (snippets-in-path root))]
    (spit output-file
          (with-out-str
            (pprint
             (assoc project
               :namespaces namespaces
               :snippets snippets))))))
