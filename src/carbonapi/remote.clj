(ns carbonapi.remote
  (:require [clj-http.client :as client]
            [flatland.protobuf.core :refer [protodef protobuf-load]])
  (:import (carbonzipperpb Carbonzipper$MultiFetchResponse))
)

(def MultiFetchResponse (protodef Carbonzipper$MultiFetchResponse))

(defonce source-host (atom nil))
(defonce source-format (atom "json"))

(defn- normalize-pb-metric [metric]
  (let [target (:name metric)
        start-time (.valAt metric "startTime")
        step-time (.valAt metric "stepTime")]
    {
     :target target
     :datapoints (map-indexed (fn [idx item]
                                [item (+ start-time (* step-time idx))])
                              (:values metric))
     }))

(defn- normalize-pb-metrics [metrics]
  (map normalize-pb-metric (:metrics metrics)))

(defn set-host! [host]
  (reset! source-host host))

(defn set-format! [format]
  (reset! source-format format))

(defn- build-query [targets format]
  (str
   @source-host
   "/render?format="
   format
   "&"
   (clojure.string/join "&" (map (partial str "target=") targets))))

(defn- fetch-json [targets]
  (:body (client/get (build-query targets "json") {:as :json})))

(defn- fetch-protobuf [targets]
  (normalize-pb-metrics (protobuf-load MultiFetchResponse (:body (client/get (build-query targets "protobuf") {:as :byte-array})))))

(defn fetch [targets]
  (if (nil? @source-host)
    (println "carbonapi.remote/source-host is not set, use set-host! for this")
    (case @source-format
      "json" (fetch-json targets)
      "protobuf" (fetch-protobuf targets)
      )))
