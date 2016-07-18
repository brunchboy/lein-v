;;; A lein-v implementation of Maven version 3
;;; It supports three levels of numeric versions (major, minor & patch).  Commit distance
;;; is represented by the Maven build number (e.g. 1.2.3-9) when there is no qualifier, and
;;; by a trailing, dash-separated numeric qualifier (practically, a build number) in the presence
;;; of a qualifier.  Qualifiers may have up to nine releases, e.g. beta3, alpha4 and are considered
;;; one-based with the first release (release 1) not printing its release number.
;;; SNAPSHOT qualifiers are allowed and when a SNAPSHOT version is in effect, commit
;;; distance as a build number is suppressed.  Effectively many commits can have the same
;;; (SNAPSHOT) version.
;;; http://maven.apache.org/ref/3.2.5/maven-artifact/index.html#
;;; http://maven.apache.org/ref/3.3.9/maven-artifact/apidocs/org/apache/maven/artifact/versioning/ComparableVersion.html
;;; https://cwiki.apache.org/confluence/display/MAVENOLD/Versioning
;;; Example Versions & Interpretations:
;;; 1.2.3-rc4 => major 1, minor 2, patch 3, qualifier rc incremented to 4
;;; NB: java -jar ~/.m2/repository/org/apache/maven/maven-artifact/3.2.5/maven-artifact-3.2.5.jar <v1> <v2> ...<vn>
(ns leiningen.v.maven
  "An implementation of lein-v version protocols that complies with Maven v3"
  (:import [java.lang Comparable]
           [org.apache.maven.artifact.versioning ComparableVersion DefaultArtifactVersion])
  (:require [clojure.string :as string]
            [leiningen.v.version.protocols :refer :all]))

(defn- string->qualifier
  [qstring]
  (let [[_ base i] (re-matches #"(\D+)(\d)*" qstring)
        i (or (and i (Integer/parseUnsignedInt i)) 1)] ; => max revisions = 9
    [base i]))

(defn- qualifier->string
  [[qs qn]]
  (str qs (when (> qn 1) qn)))

(defn- qualify*
  [[qs qn] qualifier]
  (if (and (= qs qualifier) (not= qs "SNAPSHOT")) [qs (inc qn)] [qualifier 1]))

(defn- to-string [subversions qualifier & [distance sha dirty?]]
  (cond-> (string/join "." subversions)
    qualifier (str "-" (qualifier->string qualifier))
    (and distance (pos? distance)) (str "-" distance "-0x" sha)
    dirty? (str "-DIRTY")))

(deftype MavenVersion [subversions qualifier distance sha dirty?]
  Object
  (toString [this] (to-string subversions qualifier distance sha dirty?))
  Comparable
  (compareTo [this that] ; Need to suppress SHA for purposes of comparison
    (compare (DefaultArtifactVersion. (to-string subversions qualifier distance nil dirty?))
             (let [subversions (.subversions that)
                   qualifier (.qualifier that)
                   distance (.distance that)
                   dirty? (.dirty? that)]
               (DefaultArtifactVersion. (to-string subversions qualifier distance nil dirty?)))))
  SCMHosted
  (tag [this] (to-string subversions qualifier))
  (distance [this] distance)
  (sha [this] sha)
  (dirty? [this] dirty?)
  Releasable
  (release [this level]
    (condp contains? level
      #{:major :minor :patch} (let [l ({:major 0 :minor 1 :patch 2} level)
                                    subversions (map-indexed (fn [i el] (cond (< i l) el
                                                                             (= i l) (inc el)
                                                                             (> i l) 0)) subversions)]
                                (MavenVersion. (vec subversions) nil 0 sha dirty?))
      #{:alpha :beta :rc} (MavenVersion. subversions (qualify* qualifier (name level)) 0 sha dirty?)
      #{:snapshot} (MavenVersion. subversions (qualify* qualifier "SNAPSHOT") 0 sha dirty?)
      #{:release} (do (assert qualifier "There is no pre-release version pending")
                      (MavenVersion. subversions nil 0 sha dirty?))
      (throw (Exception. (str "Not a supported release operation: " level))))))

(defn- parse-tag [vstring]
  (let [[sstr qstr & _] (string/split vstring #"-")
        subversions (into [] (map #(Integer/parseInt %)) (string/split sstr #"\."))
        qualifier (and qstr (string->qualifier qstr))]
    [subversions qualifier]))

(defn from-scm
  ([] (MavenVersion. [0 0 0] nil nil 0 nil))
  ([tag distance sha dirty?]
   (if tag
     (let [[subversions qualifier] (parse-tag tag)]
       (MavenVersion. subversions qualifier distance sha dirty?))
     (MavenVersion. [0 0 0] nil distance sha dirty?))))
