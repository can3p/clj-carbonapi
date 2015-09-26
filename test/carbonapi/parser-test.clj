(ns carbonapi.parser-test
  (:require [carbonapi.parser :refer :all]
            [clojure.test :refer :all]))

(deftest parse-target-round
  (let [target "target.{1,2}_record*.*"]
    (is (= target (build-target (-> target decompose-target second)))
        "parser should be able to construct target back")))

(deftest target-matching
  (let [target "target.{1,2}_record*.*"
        matcher (-> target decompose-target second build-matcher)
        targets [
                 ["target.1_record.two" true]
                 ["target.2_recordbla.two" true]
                 ["target.3_record.two" false]
                 ["target.2_recordbla" false]
                 ]]
    (doseq [[trg should-match?] targets]
      (is (= (matches? matcher trg) should-match?) (str "matching " trg "should result in " should-match?))
      )))

(deftest targets-extraction
  (let [target "abs(target.{1,2}_record*.*,2,test.count)"
        [targets tree] (parse-query target)
        targets-set #{"target.{1,2}_record*.*" "test.count"}]
    (is (= targets-set (set targets)) "parse-query should extract targets from arguments")))
