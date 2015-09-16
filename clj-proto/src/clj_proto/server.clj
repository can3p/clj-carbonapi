(ns clj-proto.server
  (:import [javax.imageio ImageIO]
           [java.io ByteArrayOutputStream ByteArrayInputStream]
           [org.jfree.chart JFreeChart]
           [java.awt.image BufferedImage])
  (:require [compojure.core :refer :all]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.util.response :refer [response]]
            [compojure.response :refer [Renderable render]]
            [org.httpkit.server :refer [run-server]]))

(defn- map-keys [func hash]
  (let [arr (into [] hash)]
    (reduce (fn [acc [key val]]
              (assoc acc (func key) val)) {} arr)))

(defn- render-image [image]
  (let [out (ByteArrayOutputStream.)]
    (do (ImageIO/write image "png" out) (ByteArrayInputStream. (.toByteArray out)))))

(extend-protocol Renderable
  JFreeChart
  (render [image _]
    (-> (ring.util.response/response (render-image (.createBufferedImage image 500 300)))
        (ring.util.response/content-type "image/png"))))

(defn- render-route [req]
  (let [qparams (map-keys keyword (:query-params req))
        targets (-> qparams
                    :target
                    list
                    flatten)
        params (select-keys qparams
                            [:width :height :title])
        result (clj-proto.core/render-query targets)]
    (if (= "json" (:format qparams))
      (response (clj-proto.core/query targets))
      (clj-proto.core/render-query targets))))

(defroutes app
  (GET "/render" [] render-route))

(defonce server (atom nil))

(defn stop []
  (when-not (nil? @server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@server :timeout 100)
    (reset! server nil)))

(defn start []
  (reset! server (run-server (-> app
                                 wrap-params
                                 wrap-json-response)
                             {:port 5000})))

