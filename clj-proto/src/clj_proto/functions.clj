(ns clj-proto.functions
  (:require [clj-proto.math :refer [abs]]))

(defn- get-arg [arg]
  (cond
    (map? arg) (:target arg)
    (string? arg) (str "\"" arg "\"")
    :else (str arg)))

(defn- func-name [name & args]
  (str name
       "("
       (clojure.string/join ", " (map get-arg args))
       ")"))

(defn- map-values [func series fname]
  (let [data (:datapoints series)
        app-func (fn [[val ts]] [(func val) ts])
        new-data (map app-func data)]
    (assoc series
           :target (fname series)
           :datapoints new-data)))

(defn- map-series [func series-lists fname]
  (map (fn [series-list]
         (map-values func series-list fname))
       series-lists))

(defn call-func [func & args]
  (let [f (resolve (symbol (str "clj-proto.functions/" func)))]
    (apply f args)))

(defn absolute [series-lists]
  (let [fname (fn [series-list] (func-name "absolute" series-list))]
    (map-series abs series-lists fname)))

(defn offset [series-lists value]
  (let [fname (fn [series-list] (func-name "offset" series-list value))]
    (map-series (partial + value) series-lists fname)))
