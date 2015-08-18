(ns clj-proto.functions
  (:require [clj-proto.math :refer [abs]]))

(defn get-arg [arg]
  (cond
    (map? arg) (:target arg)
    (string? arg) (str "\"" arg "\"")
    :else (str arg)))

(defn func-name [name & args]
  (str name
       "("
       (clojure.string/join ", " (map get-arg args))
       ")"))

(defn map-values [func series]
  (let [data (:datapoints series)
        target (:target series)
        app-func (fn [[val ts]] [(func val) ts])
        new-data (map app-func data)]
    (assoc series
           :target (func-name "abs" series)
           :datapoints new-data)))

(defn absolute [series-list]
  (map-values abs))
