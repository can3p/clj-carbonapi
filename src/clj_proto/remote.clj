(ns clj-proto.remote
  (:require [clj-http.client :as client]))


(def source-host "http://localhost:4000/render")

(defn- build-query [targets]
  (str
   source-host
   "?format=json&"
   (clojure.string/join "&" (map (partial str "target=") targets))))

(defn fetch [targets]
  (:body (client/get (build-query targets) {:as :json})))
