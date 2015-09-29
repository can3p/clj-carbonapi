(ns carbonapi.main
  (:require [clojure.tools.cli :refer [cli]]
            [carbonapi.server])
  (:gen-class))

(defn- exit [status msg]
  (println msg)
  (System/exit status))

(defn- start-server []
  (println "Starting server on localhost:5000")
  (carbonapi.server/start!))

(defn -main [& args]
  (let [[opts args banner] (cli args
                                ["-h" "--help" "Clojure graphite-api wannabe"
                                 :default false :flag true])]
    (cond
      (:help opts) (exit 0 banner)
      :else (start-server))))
