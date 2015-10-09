(defproject clj-carbonapi "0.1.0"
  :description "Clojure version of carbon-api"
  :url "https://github.com/can3p/clj-carbon-api"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main carbonapi.main
  :aot [carbonapi.main]
  :plugins [[lein-protobuf "0.4.3"]]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.cli "0.3.3"]
                 [compojure "1.4.0"]
                 [http-kit "2.1.18"]
                 [cheshire "5.5.0"]
                 [instaparse "1.4.1"]
                 [clj-http "2.0.0"]
                 [ring/ring-json "0.4.0"]
                 [incanter "1.5.6"]
                 [org.beatlevic/protobuf "0.8.2"]
                 ;;[org.flatland/protobuf "0.8.2"]
                 ])
