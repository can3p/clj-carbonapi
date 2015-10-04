(ns carbonapi.remote
  (:require [clj-http.client :as client]))


(defonce source-host (atom nil))

(defn set-host! [host]
  (reset! source-host host))

(defn- build-query [targets]
  (str
   @source-host
   "/render?format=json&"
   (clojure.string/join "&" (map (partial str "target=") targets))))

(defn fetch [targets]
  (if (nil? @source-host)
    (println "carbonapi.remote/source-host is not set, use set-host! for this")
    (:body (client/get (build-query targets) {:as :json}))
    ))
