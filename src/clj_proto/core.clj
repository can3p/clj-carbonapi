(ns clj-proto.core)

(defn- unique [l] (apply list (set l)))

(defn- concat-sources [sources]
  (unique
   (apply concat sources)))

(defn query [queries]
  (let [parsed (map clj-proto.parser/parse-query queries)
        sources (concat-sources (map first parsed))
        trees (map second parsed)
        targets (clj-proto.remote/fetch sources)
        eval-tree (partial clj-proto.parser/eval-tree targets)]
    (apply concat
           (map eval-tree trees))))

(defn render-query [queries & options]
  (-> (query queries)
      (clj-proto.chart/draw-series)))

(defn view-query [queries & options]
  (-> (query queries)
      (clj-proto.chart/draw-series)
      (clj-proto.chart/view)))


(defn query-single [query-str]
  (query [query-str]))

(defn render-query-single [query-str]
  (render-query [query-str]))

(defn view-query-single [query-str & options]
  (-> (query-single query-str)
      (clj-proto.chart/draw-series)
      (clj-proto.chart/view)))
