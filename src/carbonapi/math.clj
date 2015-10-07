(ns carbonapi.math)

(defn abs
  "Retuns an absolute value of the argument"
  [n]
  (max n (- n)))
