(ns carbonapi.remote
  (:require [clj-http.client :as client]
            [flatland.protobuf.core :refer [protodef protobuf-load]])
  (:import (carbonzipperpb Carbonzipper$MultiFetchResponse))
)

(def MultiFetchResponse (protodef Carbonzipper$MultiFetchResponse))

;; these atoms are needed to avoid continues passing of these parameters
;; through all functions as parameters.
(defonce source-host (atom nil))
(defonce source-format (atom "json"))

(defn- normalize-pb-metric
  "Takes protobuf output for a single metric and transforms it to the same format as with json"
  [metric]
  (let [target (:name metric)
        start-time (.valAt metric "startTime")
        step-time (.valAt metric "stepTime")]
    {
     :target target
     :datapoints (map-indexed (fn [idx item]
                                [item (+ start-time (* step-time idx))])
                              (:values metric))
     }))

(defn- normalize-pb-metrics
  "Takes zipper response in protobuf format and transforms it to the same format as with json"
  [metrics]
  (map normalize-pb-metric (:metrics metrics)))

(defn set-host!
  "Sets the host to fetch the data from. The host should be with protocol, e.g. http://localhost:8080"
  [host]
  (reset! source-host host))

(defn set-format!
  "Sets the output format to request from the zipper. Currently only json and protobuf formats are supported"
  [format]
  (reset! source-format format))

(defn- build-query
  "Builds url to fetch the data from"
  [targets format]
  (str
   @source-host
   "/render?format="
   format
   "&"
   (clojure.string/join "&" (map (partial str "target=") targets))))

(defn- fetch-json
  "Fetches data in json format"
  [targets]
  (:body (client/get (build-query targets "json") {:as :json})))

(defn- fetch-protobuf
  "Fetches and normalizes data in protobuf format"
  [targets]
  (normalize-pb-metrics (protobuf-load MultiFetchResponse (:body (client/get (build-query targets "protobuf") {:as :byte-array})))))

(defn fetch
  "Fetches data from the zipper. Function relies on the values set by set-host! and set-format! functions. Currently only json and protobuf communication formats are supported"
  [targets]
  (if (nil? @source-host)
    (println "carbonapi.remote/source-host is not set, use set-host! for this")
    (case @source-format
      "json" (fetch-json targets)
      "protobuf" (fetch-protobuf targets)
      )))
