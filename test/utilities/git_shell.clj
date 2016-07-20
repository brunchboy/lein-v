(ns utilities.git-shell
  "Utility functions for manipulating a test git repo"
  (:import [java.nio.file Files])
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]))

(defmacro shell!
  [& body]
  `(let [tmpdir# (Files/createTempDirectory
                  (.toPath (io/as-file (io/resource "tmp-git")))
                  "repo"
                  (into-array java.nio.file.attribute.FileAttribute []))
         env# {"GIT_AUTHOR_NAME" "Test User"
               "GIT_AUTHOR_EMAIL" "user@domain.com"
               "GIT_AUTHOR_DATE" "2016-11-16T22:22:22"
               "GIT_COMMITTER_NAME" "Test User"
               "GIT_COMMITTER_EMAIL" "user@domain.com"
               "GIT_COMMITTER_DATE" "2016-11-16T22:22:22"}]
     (shell/with-sh-dir (str tmpdir#)
       (shell/with-sh-env env#
         ~@body))))

(defn- sh [command] (let [result (shell/sh "/bin/bash" "-c" command)]
                      (assert (->  result :exit zero?) (:err result))))

(defn init! [] (sh "git init"))

(defn commit! [] (sh "git commit -m \"Commit\" --allow-empty"))

(defn tag! [t] (sh (format "git tag -a -m \"R %s\" %s" t t)))

(defn dirty! [] (sh "echo \"Hello\" >> x && git add x"))

;; Create a bundle with: `git bundle create my.repo --all`
(defn clone! [bundle] (sh (format "git clone %s . -b master" (-> bundle io/resource io/as-file str))))
