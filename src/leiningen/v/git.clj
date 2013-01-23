(ns leiningen.v.git
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]))

;; TODO: switch to jgit instead of shelling out
(let [shell "/bin/bash"
      cmd [shell "-c"]]
  (defn- git-command
   ([command] (git-command command ".git"))
   ([command git-dir]
      (when (.exists (io/file git-dir))
        (let [cmd (conj cmd (str "git --git-dir=" git-dir \space command))
              {:keys [exit out]} (apply shell/sh cmd)]
          (when (zero? exit) (string/split-lines out)))))))

(defn- git-status []
  (git-command "status -b --porcelain"))

(defn- git-describe []
  (git-command "describe --match 'v*.*' --abbrev=4 --dirty=**DIRTY**"))

;; Maven version formats are a cluster fuck.  The references below
;; make that very clear.  This plugin adopts the following convention
;; for leiningen/maven versions:
;;      <major>.<minor>[.<revision>][-<qualifier>][-<build> | -SNAPSHOT]
;; Where major, minor, revision and build are integers, qualifier is a
;; alphanumeric string starting with a lowercase letter and SNAPSHOT is the
;; literal string "SNAPSHOT".  Git tags should be of the form
;;      v<major>.<minor>[.<revision>][-<qualifier>][-SNAPSHOT]
;; The build component is determined by the commit distance from the
;; nearest version tag.  The use of SNAPSHOT implies that commit
;; distance is ignored.  Examples:
;; dist, tag               => version
;;   0, v1.0-beta          => 1.0.beta
;;   2, v1.0-beta          => 1.0.beta-2
;;   2, v1.0-beta-SNAPSHOT => 1.0.beta-SNAPSHOT
;;   0, v2.1.1             => 2.1.1
;;   5, v2.1.1             => 2.1.1-5
;;   0, v2.1.1-SNAPSHOT    => 2.1.1-SNAPSHOT
;;   9, v2.1.1             => 2.1.1-SNAPSHOT
;; http://docs.codehaus.org/display/MAVEN/Dependency+Mediation+and+Conflict+Resolution
;; http://dev.clojure.org/display/doc/Maven+Settings+and+Repositories
;; http://maven.40175.n5.nabble.com/How-to-use-SNAPSHOT-feature-together-with-BETA-qualifier-td73263.html
(let [re #"^v((\d+)\.(\d+)(?:\.(\d+))?(?:-([a-z]\w*))?(-SNAPSHOT)?)(?:-(\d+)-(?:g[^\*]{4,}))?(\*\*DIRTY\*\*)?$"]
  (defn version [project]
    (when-let [v (first (git-describe))]
      (let [[_ mmrqs major minor revision qualifier snapshot build dirty] (re-find re v)]
        (cond
         dirty dirty
         snapshot mmrqs
         build (str mmrqs "-" build)
         mmrqs mmrqs
         :else "0.0-SNAPSHOT")))))

(defn workspace-state [project]
  (when-let [status (git-status)]
    {:status {:tracking (filter #(re-find #"^##\s" %) status)
              :files (remove #(re-find #"^##\s" %) status)}
     :describe (first (git-describe))}))