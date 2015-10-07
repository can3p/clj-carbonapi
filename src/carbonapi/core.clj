(ns carbonapi.core
  (:require [carbonapi.parser]
            [carbonapi.remote]
            [carbonapi.chart]))

(defn- unique
  "Strip all duplicate entities from the sequence"
  [l]
  (apply list (set l)))

(defn- concat-sources
  "Takes a sequence of arrays with sources, transforms to a plain list and removes duplicates"
  [sources]
  (unique
   (apply concat sources)))

(defn query
  "Takes an array of queries, fetches them from carbonzipper, applies necessary functions and returns a resulting set"
  [queries]
  (let [parsed (map carbonapi.parser/parse-query queries)
        sources (concat-sources (map first parsed))
        trees (map second parsed)
        targets (carbonapi.remote/fetch sources)
        eval-tree (partial carbonapi.parser/eval-tree targets)]
    (apply concat
           (map eval-tree trees))))

(defn render-query
  "Takes an array of queries, fetches them from carbonzipper, applies necessary functions and returns a graph with results"
  [queries & [options]]
  (-> (query queries)
      (carbonapi.chart/draw-series options)))

(defn view-query
  "Takes an array of queries, fetches them from carbonzipper, applies necessary functions and opens a window with resulting graph"
  [queries & [options]]
  (-> (query queries)
      (carbonapi.chart/draw-series options)
      (carbonapi.chart/view)))


(defn query-single
  "Takes an single query, fetches it from carbonzipper, applies necessary functions and returns a resulting set"
  [query-str]
  (query [query-str]))

(defn render-query-single
  "Takes an single query, fetches it from carbonzipper, applies necessary functions and returns a graph with results"
  [query-str & [options]]
  (render-query [query-str] options))

(defn view-query-single
  "Takes an single query, fetches it from carbonzipper, applies necessary functions and opens a window with resulting graph"
  [query-str & [options]]
  (-> (query-single query-str)
      (carbonapi.chart/draw-series options)
      (carbonapi.chart/view)))
