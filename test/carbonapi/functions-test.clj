(ns carbonapi.functions-test
  (:require [carbonapi.functions :refer :all]
            [clojure.test :refer :all]))

(deftest absolute-test
  (let [metrics [
                 {
                  :target "test.1"
                  :datapoints [
                               [1 1]
                               [nil 1]
                               [-5 3]
                               ]
                  }
                 {
                  :target "test.2"
                  :datapoints [
                               [-1 1]
                               [nil 1]
                               [-5 3]
                               ]
                  }
                 ]
        results '(
                 {
                  :target "absolute(test.1)"
                  :datapoints [
                               [1 1]
                               [nil 1]
                               [5 3]
                               ]
                  }
                 {
                  :target "absolute(test.2)"
                  :datapoints [
                               [1 1]
                               [nil 1]
                               [5 3]
                               ]
                  }
                 )
        ]
    (is (= results (absolute metrics)) "absolute function")))

(deftest offset-test
  (let [metrics [
                 {
                  :target "test.1"
                  :datapoints [
                               [1 1]
                               [nil 2]
                               [-5 3]
                               ]
                  }
                 {
                  :target "test.2"
                  :datapoints [
                               [-1 1]
                               [nil 2]
                               [-5 3]
                               ]
                  }
                 ]
        results '(
                 {
                  :target "offset(test.1,2)"
                  :datapoints [
                               [3 1]
                               [nil 2]
                               [-3 3]
                               ]
                  }
                 {
                  :target "offset(test.2,2)"
                  :datapoints [
                               [1 1]
                               [nil 2]
                               [-3 3]
                               ]
                  }
                 )
        ]
    (is (= results (offset metrics 2)) "offset function")))
