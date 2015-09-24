(ns carbonapi.parser-test
  (:require [carbonapi.parser :refer :all]
            [clojure.test :refer :all]))

(deftest parse-target-round
  (let [target "target.{1,2}_record*.*"]
    (is (= target (build-target (-> target decompose-target second)))
        "parser should be able to construct target back")))
