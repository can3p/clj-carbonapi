(ns carbonapi.functions
  (:require [carbonapi.math :refer [abs]]))

(defn- get-arg
  "Returns serialized form of argument"
  [arg]
  (cond
    (map? arg) (:target arg)
    (string? arg) (str "\"" arg "\"")
    :else (str arg)))

(defn- func-name
  "Reconstructs function call from name and a list of arguments"
  [name & args]
  (str name
       "("
       (clojure.string/join "," (map get-arg args))
       ")"))

(defn- map-values
  "Applies function to every value of the single series and returns new series with target changed to reflect function application"
  [func series fname]
  (let [data (:datapoints series)
        app-func (fn [[val ts]] [(if (nil? val) nil (func val)) ts])
        new-data (map app-func data)]
    (assoc series
           :target (fname series)
           :datapoints new-data)))

(defn- map-series
  "Applies function to every value for a series lists and return new series lists with targets changed to reflect function application"
  [func series-lists fname]
  (map (fn [series-list]
         (map-values func series-list fname))
       series-lists))

(defn call-func
  "Function is called by parser to execute function on a list. Currently it searches for the function name in the carbonapi.functions namespace and executes the function if found"
  [func & args]
  (let [f (resolve (symbol (str "carbonapi.functions/" func)))]
    (apply f args)))

(defn absolute
  "Transforms all values of the series lists to their absolute value"
  [series-lists]
  (let [fname (fn [series-list] (func-name "absolute" series-list))]
    (map-series abs series-lists fname)))

(defn offset
  "Shifts all the values of the series lists by a value"
  [series-lists value]
  (let [fname (fn [series-list] (func-name "offset" series-list value))]
    (map-series (partial + value) series-lists fname)))
