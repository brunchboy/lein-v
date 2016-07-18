;;; A lein-v implementation of Semantic Version 2.0.0.
;;; It supports major and minor releases and implicit patch releases based on the commit
;;; distance from the last major/minor version-tagged commit.  SCM tags are, however, of the
;;; form "major.minor.0" to be more aesthetically pleasing and consistent with the standard.
;;; SHA metadata is added for positive commit distances, and a "DIRTY" metadatum is added when
;;; approprite.  There is no support for Semantic Version's pre-releases!  The ordering/
;;; precedence rules cannot be reconciled with the automatic assignment of patch releases.
;;; http://semver.org/spec/v2.0.0.html
(ns leiningen.v.semver
  "An implementation of lein-v version protocols that complies with Semantic Versioning 2.0.0"
  (:require [clojure.string :as string]
            [leiningen.v.version.protocols :refer :all]))

(deftype SemVer [subversions distance sha dirty?]
  Object
  (toString [this] (let [be (string/join "." (conj subversions distance))
                         metadata (string/join "." (cond-> []
                                                     (and distance (pos? distance)) (conj (str "0x" sha))
                                                     dirty? (conj "DIRTY")))]
                     (cond-> be
                       (not (string/blank? metadata)) (str "+" metadata))))
  Comparable
  (compareTo [this that] (compare [(vec (.subversions this)) (.distance this) (.dirty? this)]
                                  [(vec (.subversions that)) (.distance that) (.dirty? that)]))
  SCMHosted
  (tag [this] (string/join "." (conj subversions distance)))
  (distance [this] distance)
  (sha [this] sha)
  (dirty? [this] dirty?)
  Releasable
  (release [this level]
    (condp contains? level
      #{:major :minor} (let [l ({:major 0 :minor 1} level)
                             subversions (map-indexed (fn [i el] (cond (< i l) el
                                                                      (= i l) (inc el)
                                                                      (> i l) 0)) subversions)]
                         (SemVer. (vec subversions) 0 sha dirty?))
      #{:patch} (throw (Exception. "Patch releases are implicit by commit distance"))
      (throw (Exception. (str "Not a supported release operation: " level))))))

(let [re #"(\d+)\.(\d+)\.(\d+)"]
  (defn- parse-base [base]
    (let [[_ major minor patch] (re-matches re base)]
      (assert (= "0" patch) "Non-zero patch level found in SCM base")
      (mapv #(Integer/parseInt %) [major minor]))))

(defn from-scm
  ([] (SemVer. [0 1] 0 nil nil))
  ([base distance sha dirty?]
   (if base
     (let [subversions (parse-base base)]
       (SemVer. subversions distance sha dirty?))
     (SemVer. [0 1] distance sha dirty?))))
