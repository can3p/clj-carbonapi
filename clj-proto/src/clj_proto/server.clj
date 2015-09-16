(ns clj-proto.server
  (:import [javax.imageio ImageIO]
           [java.io ByteArrayOutputStream ByteArrayInputStream])
  (:require [compojure.core :refer :all]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.util.response :refer [response]]
            [org.httpkit.server :refer [run-server]]))

(defn- map-keys [func hash]
  (let [arr (into [] hash)]
    (reduce (fn [acc [key val]]
              (assoc acc (func key) val)) {} arr)))

(defn- render-image [image]
  (let [out (ByteArrayOutputStream.)]
    (do (ImageIO/write image "png" out) (ByteArrayInputStream. (.toByteArray out)))))

(defn- response-chart [chart options]
  (let [w (Integer/parseInt (or (:width options) "500"))
        h (Integer/parseInt (or (:height options) "300"))
        img (.createBufferedImage chart w h)]
  (-> (ring.util.response/response (render-image img))
      (ring.util.response/content-type "image/png"))))

(defn- render [req]
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
      (response-chart (clj-proto.core/render-query targets) params))))

(defroutes app
  (GET "/render" [] render))

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

