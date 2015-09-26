(ns carbonapi.parser
  (:require [clj-http.client :as client]
            [instaparse.core :as insta]))

(def parser
  (insta/parser
   "source = target | func
    func = #'[a-zA-Z]+' <'('> argument (<','> argument)* <')'>
    argument = <opt-sp> ( number | string | target | func) <opt-sp>
    string = #'\"[a-zA-Z0-9_-]+\"'
    number = #'\\d+'
    opt-sp = #'[ ]*'
    target = first-chunk ( <'.'> target-star* chunk )*
    chunk = ( target-literal | target-star | target-enum | target-range )+
    first-chunk = ( first-target-literal | target-star | target-enum | target-range )+
    first-target-literal = #'[a-zA-Z][a-zA-Z0-9_]*'
    target-literal = #'[a-zA-Z0-9_]+'
    target-star = '*'
    target-range = <'['> #'[a-zA-Z_-]+' <']'>
    target-enum = <'{'> target-literal (<','>  target-literal )* <'}'>
"))

(def parser-transform {
                       :argument identity
                       :source identity
                       :number read-string
                       :string read-string
                       :func (fn [& args]
                               (conj args :call-func))
                       :chunk (fn [& args] args)
                       :target (fn [& args]
                                 (list :grep-target args))
                       :first-chunk (fn [& args] args)
                       :first-target-literal (fn [& args]
                                               (vec (conj args :target-literal)))
                       })

(defn decompose-target [source]
  (->> (parser source) (insta/transform parser-transform)))

;; (decompose-target "abs(avg(test.*, 2), \"abc\")") will result in
;; ("abs" ("avg" [:target "test.*"] 2) "abc")

(defn- build-target-chunk [els]
  (let [build (fn [[symb arg :as args]]
                (case symb
                  :target-literal arg
                  :target-star "*"
                  :target-enum (str "{"
                                    (clojure.string/join
                                     ","
                                     (map second (rest args)))
                                    "}")))]
    (apply str (map build els))))

(defn build-target [l]
  (clojure.string/join
   "."
   (map build-target-chunk l)))

(defn- build-matcher-chunk [els]
  (let [build (fn [[symb arg :as args]]
                (case symb
                  :target-literal arg
                  :target-star "[^.]*"
                  :target-enum (str "("
                                    (clojure.string/join
                                     "|"
                                     (map second (rest args)))
                                    ")")))]
    (apply str (map build els))))

(defn build-matcher [l]
  (re-pattern
   (clojure.string/join
    "\\."
    (map build-matcher-chunk l))))

(defn matches? [pattern val]
  (not (nil? (re-find pattern val))))

(defn- grep-target [targets target]
  (let [ptrn (build-matcher target)]
    (filter (fn [t]
              (matches? ptrn
                        (:target t)))
            targets)))

(defn- eval-tree-custom [t eval-map]
  (let [eval-func (fn [form]
                    (cond
                      (and (seq? form) (contains? eval-map (first form)))
                      (apply (eval-map (first form)) (rest form))
                      :else form
                      ))]
    (clojure.walk/postwalk eval-func t)))

(defn eval-tree [targets t]
  (let [eval-map {
                  :call-func carbonapi.functions/call-func
                  :grep-target (partial grep-target targets)
                  }]
    (eval-tree-custom t eval-map)))

(defn- extract-targets [tree]
  (->> tree
       (tree-seq seq? identity)
       (filter #(and (seq? %) (= :grep-target (first %))))
       (map (comp build-target second))))

(defn parse-query [t]
  (let [tree (decompose-target t)]
    [(extract-targets tree) tree]))
