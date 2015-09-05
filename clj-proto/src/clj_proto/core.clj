(ns clj-proto.core)

(defn query [query-str]
  (let [[sources tree] (clj-proto.parser/parse-query query-str)
        targets (clj-proto.remote/fetch sources)]
    (clj-proto.parser/eval-tree tree targets)))

(defn view-query [query-str]
  (-> (query query-str)
      (clj-proto.chart/draw-series)
      (clj-proto.chart/view)))
