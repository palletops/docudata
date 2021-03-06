(ns com.palletops.docudata.extract
  "Extract documentation data from code."
  (:require
   [clojure.java.io :refer [file]]
   [bultitude.core :as bultitude])
  (:import
   java.io.File))

(defn clj-files
  "Return a lazy sequence of clojure files from `paths`."
  [paths]
  (->> paths
       (map file)
       (mapcat file-seq)
       (filter #(.isFile ^File %))
       (filter #(.endsWith (.getPath ^File %) ".clj"))))

(defn clj-namespaces
  "Return a lazy sequence of namespaces from files in `paths`."
  [paths]
  (mapcat bultitude/namespaces-in-dir paths))

(defn require-ns
  "Require the namespace named by `ns-sym`, returning `ns-sym` on success,
  or throwing an exception otherwise."
  [ns-sym]
  (try
    (require ns-sym)
    ns-sym
    (catch Exception e
      (throw
       (ex-info
        (str "Failed to load namespace " ns-sym)
        {}
        e)))))

(defn var-type
  "Return a keyword specifying the type of the Var `v`."
  [v]
  (let [m (meta v)
        val (var-get v)]
    (cond
     (:macro m) :macro
     (:protocol m) :protocol-method
     (:arglists m) :fn
     (and (map? val) (:on-interface val)) :protocol
     (instance? clojure.lang.MultiFn val) :multi-method
     (.contains (str (:name m)) "proxy$") :proxy
     :else :var)))

(defmulti extract-data
  (fn extract-data-dispatch [{:keys [var-type]} _] var-type))

(defmethod extract-data :default
  [m v] m)

(defmethod extract-data :protocol
  [m v] (assoc m :var v))

(defn- dissoc-keys
  [m ks]
  (apply dissoc m ks))


(def schema-explain
  (delay (ns-resolve 'schema.core 'explain)))

(defn explain-sig
  "Call explain on the elements of a :sig."
  [sig]
  (->
   (mapv (fn [s]
           (mapv (fn [x]
                   (if (= x ':-)
                     x
                     (@schema-explain x)))
                 s))
         sig)))

(defn var-data
  "Return documentation data for a var."
  [v options]
  (-> (meta v)
      (assoc :var-type (var-type v))
      (update-in [:ns] ns-name)
      (dissoc-keys (:exclude-keys options))
      (as-> m
            (cond-> m
                    (:sig m) (update-in [:sig] explain-sig)))
      (extract-data v)))

(defn protocol-with-methods
  [protocol methods]
  (-> protocol
      (assoc :methods (->> methods
                           (filter #(= (:var protocol) (:protocol %)))
                           (map #(dissoc % :protocol))))
      (dissoc :var)))

(defn protocols-with-methods
  [protocols methods]
  (map #(protocol-with-methods % methods) protocols))

(defn ns-var-data
  "Return a sequence of data on each var in the namespace `n`."
  [n options]
  (let [vs (->> (ns-interns n) vals (map #(var-data % options)))]
    (concat
     (remove (comp #{:protocol-method :protocol :proxy} :var-type) vs)
     (protocols-with-methods
      (filter (comp #{:protocol} :var-type) vs)
      (filter (comp #{:protocol-method} :var-type) vs)))))

(defn ns-data
  "Return data on a namespace."
  [ns-sym options]
  (let [n (the-ns ns-sym)]
    (-> n
        meta
        (assoc :ns-name ns-sym
               :vars (ns-var-data n options)))))

(defn docudata
  "Return documentation data on a namespaces in the filesystem `paths`."
  [paths options]
  (->> paths
       clj-namespaces
       (map require-ns)
       (map #(ns-data % options))))
