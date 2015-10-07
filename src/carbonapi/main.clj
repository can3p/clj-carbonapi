(ns carbonapi.main
  (:require [clojure.tools.cli :refer [parse-opts]]
            [carbonapi.server])
  (:gen-class))

(defn- exit
  "Prints message and exits with specified error code"
  [status msg]
  (println msg)
  (System/exit status))

(defn- usage
  "Returns general help message. Commanline switches help should be passed as an argument"
  [options-summary]
  (->> ["carbonapi, clojure version"
        ""
        "Options:"
        options-summary
        ]
       (clojure.string/join \newline)))

(defn- start-server
  "Starts server with specified parameters. Currently only remote host (:host) and communication data format (:source-format) is supported in options"
  [options]
  (do
    (println (str "Starting server on localhost:" (:port options)))
    (carbonapi.remote/set-host! (:host options))
    (carbonapi.remote/set-format! (:source-format options))
    (carbonapi.server/start! (:port options))))

(defn error-msg
  "Returns an error message"
  [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (clojure.string/join \newline errors)))

(def cli-options [
                  ["-h" "--help" "Show this help message"]
                  [nil "--host HOST" "carbonzipper host"
                   :validate [#(re-find #"^https?:\/\/\w+" %) "Should be a full host name includeing protocol"]
                   :default "http://localhost:4000"]
                  [nil "--source-format FORMAT" "carbonzipper data format to request (json and protobuf values supported)"
                   :validate [#(contains? #{"json" "protobuf"} %) "Can be only json or protobuf for now"]
                   :default "json"]
                  [nil "--port PORT" "server port"
                   :default 5000
                   :parse-fn #(Integer/parseInt %)]
                  ])

(defn -main [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) (exit 1 (usage summary))
      errors (exit 1 (error-msg errors))
      :else (start-server options))))
