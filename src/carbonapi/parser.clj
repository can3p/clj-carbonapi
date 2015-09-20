(ns carbonapi.parser
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
    target = first-target-literal ( target-dot target-star* (target-literal target-star*)* )*
    first-target-literal = #'[a-zA-Z][a-zA-Z0-9_]*'
    target-literal = #'[a-zA-Z0-9_]+'
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
                       :first-target-literal identity
                       :target-literal identity
                       :target-star identity
                       :target (fn [& args]
                                 (list :grep-target (apply str args)))
                       })

(defn- decompose-target [source]
  (->> (parser source) (insta/transform parser-transform)))

;; (decompose-target "abs(avg(test.*, 2), \"abc\")") will result in
;; ("abs" ("avg" [:target "test.*"] 2) "abc")

(defn- extract-targets-flat [[h & r]]
  (cond
    (= nil h) nil
    (= h :grep-target) (conj (extract-targets-flat (rest r)) (first r))
    :else (extract-targets-flat r)))

(defn- extract-targets [tree]
  (let [f-list (flatten tree)]
    (extract-targets-flat f-list)))

(defn parse-query [t]
  (let [tree (decompose-target t)]
    [(extract-targets tree) tree]))

(defn matches? [pattern val]
  (not (nil? (re-find pattern val))))

(defn- grep-target [targets target]
  (let [ptrn (re-pattern (clojure.string/replace target "*" "[^.]+"))]
    (filter (fn [t]
              (matches? ptrn
                        (:target t)))
            targets)))

(defn eval-tree [targets t]
  (let [eval-map {
                  :call-func carbonapi.functions/call-func
                  :grep-target (partial grep-target targets)
                  }
        eval-func (fn [form]
                    (cond
                      (and (seq? form) (contains? eval-map (first form)))
                      (apply (eval-map (first form)) (rest form))
                      :else form
                      ))]
    (clojure.walk/postwalk eval-func t)))
