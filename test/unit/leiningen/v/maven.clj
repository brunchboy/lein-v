(ns unit.leiningen.v.maven
  (:require [leiningen.v.maven :refer [parse ->MavenVersion]]
            [leiningen.v.version.protocols :refer :all]
            [midje.sweet :refer :all]
            [midje.checking.core :refer [extended-=]]))

(defchecker as-string
  [expected]
  (checker [actual]
           (extended-= (str actual) expected)))

(fact "Can render as a string"
  (str (->MavenVersion [1 2 3] ["qualifier" 5] 3 "eadaa")) => "1.2.3-qualifier5-3-0xeadaa")

(fact "Can create"
  (parse "1.2.3-beta5-45-x12bc") => (partial instance? leiningen.v.maven.MavenVersion))

(fact "Can increment levels"
  (levels (parse "1.2.3")) => pos?
  (level++ (parse "1.2.3") 2) => (as-string "1.2.4")
  (level++ (parse "1.2.3") 1) => (as-string "1.3.0")
  (level++ (parse "1.2.3") 0) => (as-string "2.0.0")
  (level++ (parse "1.2.3") 4) => (throws java.lang.AssertionError))

(fact "Can qualify"
  (qualify (parse "1.2.3") "alpha") => (as-string "1.2.3-alpha")
  (qualifier (parse "1.2.3-q")) => "q"
  (qualified? (parse "1.2.3-q")) => truthy
  (qualified? (parse "1.2.3")) => falsey
  (release (parse "1.2.3-qualified")) => (as-string "1.2.3"))

(fact "Can increment qualifiers"
  (qualifier++ (parse "1.2.3-alpha")) => (as-string "1.2.3-alpha2")
  (qualifier++ (parse "1.2.3-alpha2")) => (as-string "1.2.3-alpha3")
  (qualifier++ (parse "1.2.3")) => (throws java.lang.AssertionError))

(fact "Can manage metadata"
  (set-metadata (parse "1.2.3") "abcd") => (as-string "1.2.3-0xabcd")
  (metadata (parse "1.2.3")) => nil?
  (metadata (parse "1.2.3-0xab12")) => "ab12"
  (clear-metadata (parse "1.2.3-0xab12")) => (as-string "1.2.3"))

(fact "Can index by distance"
  (move (parse "1.2.3") 4) => (as-string "1.2.3-4")
  (move (parse "1.2.3-2") 4) => (as-string "1.2.3-4")
  (move (parse "1.2.3-5") 4) => (throws java.lang.AssertionError)
  (move (parse "1.2.3-q") 4) => (as-string "1.2.3-q-4")
  (distance (parse "1.2.3-5")) => 5
  (base (parse "1.2.3-5")) => (as-string "1.2.3"))
