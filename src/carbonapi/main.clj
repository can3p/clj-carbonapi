(ns carbonapi.main
  (:require [clojure.tools.cli :refer [parse-opts]]
            [carbonapi.server])
  (:gen-class))

(defn- exit [status msg]
  (println msg)
  (System/exit status))

(defn- usage [options-summary]
  (->> ["carbonapi, clojure version"
        ""
        "Options:"
        options-summary
        ]
       (clojure.string/join \newline)))

(defn- start-server [options]
  (do
    (println (str "Starting server on localhost:" (:port options)))
    (carbonapi.remote/set-host! (:host options))
    (carbonapi.server/start! (:port options))))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (clojure.string/join \newline errors)))

(def cli-options [
                  ["-h" "--help" "Show this help message"]
                  [nil "--host HOST" "carbonzipper host"
                   :validate [#(re-find #"^https?:\/\/\w+" %) "Should be a full host name includeing protocol"]
                   :default "http://localhost:4000"]
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
