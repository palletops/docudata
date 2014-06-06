(ns com.palletops.docudata.extract
  "Extract documentation data from code."
  (:require
   [bultitude.core :as bultitude]
   [clojure.edn :as edn]
   [clojure.java.io :refer [file]]
   [clojure.string :refer [split-lines triml]]
   [me.raynes.fs :as fs]
   [scout.core :as scout]
   [pathetic.core :as path])
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

#_(defn ns-data
  "Return data on a namespace."
  [ns-sym options]
  (let [n (the-ns ns-sym)]
    (-> n
        meta
        (assoc :ns-name ns-sym
               :vars (ns-var-data n options)))))

(defn ns-data
  "Return data on a namespace."
  [ns-sym options]
  (let [n (the-ns ns-sym)]
    (-> n
        meta
        (assoc :ns-name ns-sym))))

(defn ns-vars [ns-sym options]
  (let [n (the-ns ns-sym)]
    (map #(assoc % :ns-name ns-sym)
        (ns-var-data n options))))

;;; extract snippets

(defn all-files
  "returns a list of all the files in the directory"
  [root]
  ;; TODO: need to remove target and other useless subdirs
  (filter fs/file?
          (fs/find-files root #".*")))

(defrecord MarkerStart [name])

(defn trim-indent
  "Given the string `s` potentially containing many lines, some with
  indentation, it trims the letf side of the text while preserving its
  indentation"
  [s]
  (let [lines (split-lines s)
        indent (fn [c]
                 (let [len-trimmed (count (triml c))
                       len (count c)]
                   (- len len-trimmed)))
        lowest-indent (apply min (map indent lines))]
    (apply str (interpose "\n" (map #(subs % lowest-indent) lines)))))

(defn splice
  "Given two matches, `statt-match` and `end-match` provided by scout,
  it returns a snipet map for the content between those two matches"
  [start-match end-match]
  (let [source (:src start-match)
        start-start-marker (-> start-match :match :start)
        end-start-marker (-> start-match :match :end)
        end-end-marker (+ end-start-marker
                          (-> end-match :match :end))
        snippet (subs source start-start-marker end-end-marker)
        _ (printf "Snippet:\n%s\n" snippet)
        snippet-lines (split-lines snippet)
        header (try (edn/read-string {:readers {'dd/start #'->MarkerStart}}
                                     (first snippet-lines))
                    (catch Exception e
                      (printf "Can't parse marker '%s'\n" (first snippet-lines))
                      nil))
        snippet (apply str (interpose \newline (rest (butlast snippet-lines))))]
    (when header
      {:name (keyword (:name header))
       :content (trim-indent snippet)})))

(defn next-snippet
  "Given a string, it parses the next snippet, returning a vector with
  the rest of the string after the parsed snippet, and the snippet
  map"
  [s]
  (let [start (-> s
                  (scout/scanner)
                  (scout/scan-until #"\#dd/start"))
        end (-> start
                (scout/remainder)
                (scout/scanner)
                (scout/scan-until #"\#dd/end"))]
    ;; full match? -> splice
    (if (and (-> start :match)
             (-> end :match))
      [(subs s (-> end :match :end)) (splice start end)]
      (if (-> start :match)
        ;; unbalanced markers
        (throw (Exception. (format "unmatched: '%s'\n" s)))
        ;; [nil nil]
        ;; no markers left
        [nil nil]))))

(defn all-snippets
  "Returns a sequence with the maps for the snippets found in the
  string `s`"
  ([s] (all-snippets s []))
  ([s snippets]
     (if (> (count s) 0)
       ;; only continue if there is some content left
       (let [[r snippet] (next-snippet s)]
         (if snippet
           (all-snippets r (conj snippets snippet))
           snippets)))))

(defn snippets-in-path
  "Returns a map of all the snippets found in a path. Each snippet is
  keyed by its name and is a map of `:content` and `:file`, the
  snippet content and the relative path to the file (from the project
  root) respectively"
  [dir]
  (let [collect-snippets
        (fn [current-snippets f]
          (let [content (try (slurp f)
                             (catch Exception e
                               (printf "Cannot read %s. Ignoring it.\n" f)
                               nil))
                snippets (all-snippets content)
                rel-path (path/relativize dir f)
                ;; add the file path to all snippet maps
                snippets (map #(assoc % :path rel-path) snippets)
                snippets-map (reduce (fn [sm m] (assoc sm
                                                 (:name m)
                                                 (dissoc m :name))) {} snippets)]
            (merge current-snippets snippets-map)))]
    (reduce collect-snippets {} (all-files dir))))

(defn docudata
  "Return documentation data on a namespaces in the filesystem `paths`."
  [paths options]
  (->> paths
       clj-namespaces
       (map require-ns)
       (map #(ns-data % options))))

(defn namespaces [paths options]
  (->> paths
       clj-namespaces
       (map require-ns)
       (map #(ns-data % options))))

(defn vars [paths options]
  (->> paths
       clj-namespaces
       (map require-ns)
       (mapcat #(ns-vars % options))))
