(ns clj-proto.core
  (:require [clj-http.client :as client]
            [instaparse.core :as insta]))

(def parser
  (insta/parser
   "source = target | func
    func = #'[a-zA-Z]+' <open-br> argument (<comma> argument)* <close-br>
    open-br = '('
    close-br = ')'
    comma = ','
    argument = <opt-sp> ( number | string | target | func ) <opt-sp>
    string = #'\"[a-zA-Z0-9_-]+\"'
    number = #'\\d+'
    opt-sp = #'[ ]*'
    target = target-literal ( target-dot target-star* (target-literal target-star*)* )*
    target-literal = #'[a-zA-Z]+'
    target-star = '*'
    target-dot = '.'
"))

(def parser-transform {
                       :argument identity
                       :source identity
                       :number read-string
                       :string read-string
                       :func (fn [& args]
                               (conj args :call-func))
                       :target-dot identity
                       :target-literal identity
                       :target-star identity
                       :target (fn [& args]
                                 (list :grep-target (apply str args)))
                       })

(defn decompose-target [source]
  (->> (parser source) (insta/transform parser-transform)))

;; (decompose-target "abs(avg(test.*, 2), \"abc\")") will result in
;; ("abs" ("avg" [:target "test.*"] 2) "abc")

(defn extract-targets-flat [[h & r]]
  (cond
    (= nil h) nil
    (= h 'grep-target) (conj (extract-targets-flat (rest r)) (first r))
    :else (extract-targets-flat r)))

(defn extract-targets [tree]
  (let [f-list (flatten tree)]
    (extract-targets-flat f-list)))

(defn expand-target [t]
  (let [tree (decompose-target t)]
    [(extract-targets tree) tree]))

(defn eval-tree [t]
  (let [eval-map {
                  :call-func (fn [& args] (println "call-func" args) (first args))
                  :grep-target (fn [& args] (println "grep-target" args) (first args))
                  }
        eval-func (fn [form]
                    (cond
                      (and (seq? form) (contains? eval-map (first form)))
                      (apply (eval-map (first form)) (rest form))
                      :else form
                      ))]
    (clojure.walk/postwalk eval-func t)))

(def source-host "http://localhost:4000/render")

(defn build-query [targets]
  (str
   source-host
   "?format=json&"
   (clojure.string/join "&" (map (partial str "target=") targets))))

(defn fetch-targets [targets]
  (:body (client/get (build-query targets) {:as :json})))

(defn abs [val]
  (if (> val 0) val (- 0 val)))

(defn matches? [pattern val]
  (not (nil? (re-find pattern val))))

(defn unique [l] (apply list (set l)))

(defn tstamp [] (quot (System/currentTimeMillis) 1000))

(defn rnd-const [const stamp]
  (if (> (rand) 0.5)
    const (- 0 const)))


(defn lookup-func [func-name]
  (let [func (resolve (symbol func-name))]
    (if (nil? func)
      identity
      func)))

(defn parse-target [target]
  (let [
        match (re-find #"(.*)\(([^)]+)\)" target)
        func (if (nil? match) identity (lookup-func (second match)))
        parsed-target (if (nil? match) target (nth match 2))
        pattern (re-pattern (clojure.string/replace parsed-target "*" "[^.]+"))
        ]
    (list parsed-target pattern func)))

(defn parse-targets [targets]
  (let [results (map parse-target targets)
        targets (unique (map first results))]
    (list results targets)))

(defn apply-matcher [[target pattern func] raw-metric]
  (if (matches? pattern (:target raw-metric))
  {
   :target (str target " - " (:target raw-metric))
   :datapoints (map (fn [[val ts]] [(func val) ts])
                    (:datapoints raw-metric))
   }
  nil
  ))

(defn apply-metrics [metrics matcher]
  (apply list (map (partial apply-matcher matcher) metrics)))

(defn get-metrics [targets]
  (let [[matchers parsed-targets] (parse-targets targets)
        raw-metrics (fetch-targets parsed-targets)
        ]
    (reduce concat (map (partial apply-metrics raw-metrics) matchers))))
