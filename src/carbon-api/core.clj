(ns carbon-api.core)

(defn- unique [l] (apply list (set l)))

(defn- concat-sources [sources]
  (unique
   (apply concat sources)))

(defn query [queries]
  (let [parsed (map carbon-api.parser/parse-query queries)
        sources (concat-sources (map first parsed))
        trees (map second parsed)
        targets (carbon-api.remote/fetch sources)
        eval-tree (partial carbon-api.parser/eval-tree targets)]
    (apply concat
           (map eval-tree trees))))

(defn render-query [queries & options]
  (-> (query queries)
      (carbon-api.chart/draw-series)))

(defn view-query [queries & options]
  (-> (query queries)
      (carbon-api.chart/draw-series)
      (carbon-api.chart/view)))


(defn query-single [query-str]
  (query [query-str]))

(defn render-query-single [query-str]
  (render-query [query-str]))

(defn view-query-single [query-str & options]
  (-> (query-single query-str)
      (carbon-api.chart/draw-series)
      (carbon-api.chart/view)))
