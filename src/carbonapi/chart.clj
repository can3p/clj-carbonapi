(ns carbonapi.chart
  (:require
   [incanter.core :as icore]
   [incanter.charts :as icharts]))

(defn- unzip [datapoints]
  (reduce (fn [points [x y]]
            (list
             (conj (first points) x)
             (conj (second points) y)
             ))
          (list [] [])
          datapoints))


(defn- prepare-data [s]
  (let [[val times-sec] (unzip (:datapoints s))
        times (map (partial * 1000) times-sec)]
    {
     :val val
     :times times
     :label (:target s)
     }))

(defn draw-series [series]
  (let [prepared (map prepare-data series)
        f (first prepared)
        r (rest prepared)
        plot (icharts/time-series-plot
              (:times f)
              (:val f)
              :series-label (:label f)
              :legend true
              :x-label ""
              :y-label ""
              )]
    (reduce (fn [p v]
              (icharts/add-lines p
                         (:times v)
                         (:val v)
                         :series-label (:label v)
                         )) plot r)))

(defn view [plot]
  (icore/view plot))
