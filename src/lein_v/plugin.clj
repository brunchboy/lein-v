(ns lein-v.plugin
  (:use [leiningen.v]))

(defn hooks []
  (deploy-when-anchored))

(defn- select-versioner [version]
  (if (= :lein-v version)
    version-from-scm
    (do (when (and (string? version) (or (empty? version) (re-find #"lein" version)))
          (println "WARNING: lein-v is not managing this project's version.  Set version in project.clj to :lein-v to trigger automatic lein-v management"))
        identity)))

(defn middleware [{version :version :as project}]
  (let [versioner (select-versioner version)]
    (add-workspace-data (versioner project))))
