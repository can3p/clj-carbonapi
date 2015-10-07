(ns carbonapi.parser
  (:require [clj-http.client :as client]
            [carbonapi.functions]
            [instaparse.core :as insta]))

;; Graphite functions grammar
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

;; Transform is necessary to remove unnecessary garbage from generated AST
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
                       ;; we do not allow a first chunk to start from digits, because it looks
                       ;; like number argument in this case, but it should not look
                       ;; differently from other chunks in the resulting AST, hence this transform
                       :first-chunk (fn [& args] args)
                       :first-target-literal (fn [& args]
                                               (vec (conj args :target-literal)))
                       })

(defn decompose-target
  "Takes a target and decomposes it to the AST"
  [source]
  (->> (parser source) (insta/transform parser-transform)))

;; (decompose-target "abs(avg(test.*, 2), \"abc\")") will result in
;; ("abs" ("avg" [:target "test.*"] 2) "abc")

(defn- build-target-chunk
  "Rebuild target chunk from the AST"
  [els]
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

(defn build-target
  "Rebuild target string from the AST"
  [l]
  (clojure.string/join
   "."
   (map build-target-chunk l)))

(defn- build-matcher-chunk
  "Builds matcher for a chunk of target"
  [els]
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

(defn build-matcher
  "Builds matcher for the target base on AST. Matcher is needed, because a single query to zipper (like server.*) may result in many series lists and we need to have a way to capture them all"
  [l]
  (re-pattern
   (clojure.string/join
    "\\."
    (map build-matcher-chunk l))))

(defn matches?
  "Checks if value matches pattern"
  [pattern val]
  (not (nil? (re-find pattern val))))

(defn- grep-target
  "Takes a list of the series returned from zipper and takes only ones that match the target specified"
  [targets target]
  (let [ptrn (build-matcher target)]
    (filter (fn [t]
              (matches? ptrn
                        (:target t)))
            targets)))

(defn- eval-tree-custom
  "Takes a tree and map of functions to apply if keys of the hash are encountered in the AST, executes them on a AST and returns a result"
  [t eval-map]
  (let [eval-func (fn [form]
                    (cond
                      (and (seq? form) (contains? eval-map (first form)))
                      (apply (eval-map (first form)) (rest form))
                      :else form
                      ))]
    (clojure.walk/postwalk eval-func t)))

(defn eval-tree
  "Takes targets fetched from zipper and AST and executes the tree"
  [targets t]
  (let [eval-map {
                  :call-func carbonapi.functions/call-func
                  :grep-target (partial grep-target targets)
                  }]
    (eval-tree-custom t eval-map)))

(defn- extract-targets
  "Takes graphite target AST and extracts targets from it"
  [tree]
  (->> tree
       (tree-seq seq? identity)
       (filter #(and (seq? %) (= :grep-target (first %))))
       (map (comp build-target second))))

(defn parse-query
  "Parses target and returns the list with targets to fetch and graphite AST"
  [t]
  (let [tree (decompose-target t)]
    [(extract-targets tree) tree]))
