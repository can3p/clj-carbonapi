(ns carbonapi.core)

(defn- unique [l] (apply list (set l)))

(defn- concat-sources [sources]
  (unique
   (apply concat sources)))

(defn query [queries]
  (let [parsed (map carbonapi.parser/parse-query queries)
        sources (concat-sources (map first parsed))
        trees (map second parsed)
        targets (carbonapi.remote/fetch sources)
        eval-tree (partial carbonapi.parser/eval-tree targets)]
    (apply concat
           (map eval-tree trees))))

(defn render-query [queries & [options]]
  (-> (query queries)
      (carbonapi.chart/draw-series)))

(defn view-query [queries & [options]]
  (-> (query queries)
      (carbonapi.chart/draw-series)
      (carbonapi.chart/view)))


(defn query-single [query-str]
  (query [query-str]))

(defn render-query-single [query-str & [options]]
  (render-query [query-str] options))

(defn view-query-single [query-str & [options]]
  (-> (query-single query-str)
      (carbonapi.chart/draw-series options)
      (carbonapi.chart/view)))
